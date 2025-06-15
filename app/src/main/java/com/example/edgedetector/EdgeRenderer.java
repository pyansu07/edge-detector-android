package com.example.edgedetector;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.egl.EGLConfig;

public class EdgeRenderer implements GLSurfaceView.Renderer {

    private int program;
    private int textureId;
    private int aPositionLocation;
    private int aTexCoordLocation;
    private int uTextureLocation;

    private ByteBuffer frameBuffer;
    private int frameWidth = 0;
    private int frameHeight = 0;
    private boolean frameAvailable = false;

    private final float[] vertexCoords = {
            -1.0f,  1.0f,
            -1.0f, -1.0f,
            1.0f,  1.0f,
            1.0f, -1.0f
    };

    private final float[] textureCoords = {
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 0.0f,
            1.0f, 1.0f
    };

    private FloatBuffer vertexBuffer;
    private FloatBuffer texBuffer;
    private boolean showEdges = true;

    public void setShowEdges(boolean show) {
        this.showEdges = show;
    }

    private final String vertexShaderCode =
            "attribute vec4 aPosition;" +
                    "attribute vec2 aTexCoord;" +
                    "varying vec2 vTexCoord;" +
                    "void main() {" +
                    "    gl_Position = aPosition;" +
                    "    vTexCoord = aTexCoord;" +
                    "}";

    private final String fragmentShaderCode =
            "precision mediump float;" +
                    "uniform sampler2D uTexture;" +
                    "varying vec2 vTexCoord;" +
                    "void main() {" +
                    "    gl_FragColor = texture2D(uTexture, vTexCoord);" +
                    "}";

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        program = createProgram(vertexShaderCode, fragmentShaderCode);
        GLES20.glUseProgram(program);

        aPositionLocation = GLES20.glGetAttribLocation(program, "aPosition");
        aTexCoordLocation = GLES20.glGetAttribLocation(program, "aTexCoord");
        uTextureLocation = GLES20.glGetUniformLocation(program, "uTexture");

        // Create OpenGL texture
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        textureId = textures[0];

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        // Vertex buffer
        vertexBuffer = ByteBuffer.allocateDirect(vertexCoords.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        vertexBuffer.put(vertexCoords).position(0);

        // Texture buffer
        texBuffer = ByteBuffer.allocateDirect(textureCoords.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        texBuffer.put(textureCoords).position(0);

        GLES20.glClearColor(0, 0, 0, 1);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        if (frameAvailable && frameBuffer != null && frameWidth > 0 && frameHeight > 0) {
            // Create Bitmap and upload to texture
            Bitmap bitmap = Bitmap.createBitmap(frameWidth, frameHeight, Config.ARGB_8888);
            frameBuffer.rewind();
            bitmap.copyPixelsFromBuffer(frameBuffer);

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
            bitmap.recycle();

            frameAvailable = false;
        }

        GLES20.glUseProgram(program);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glUniform1i(uTextureLocation, 0);

        GLES20.glEnableVertexAttribArray(aPositionLocation);
        GLES20.glVertexAttribPointer(aPositionLocation, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        GLES20.glEnableVertexAttribArray(aTexCoordLocation);
        GLES20.glVertexAttribPointer(aTexCoordLocation, 2, GLES20.GL_FLOAT, false, 0, texBuffer);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(aPositionLocation);
        GLES20.glDisableVertexAttribArray(aTexCoordLocation);
    }

    public void updateFrame(byte[] data, int width, int height) {
        this.frameWidth = width;
        this.frameHeight = height;
        int size = width * height * 4; // ARGB_8888 = 4 bytes/pixel

        if (frameBuffer == null || frameBuffer.capacity() != size) {
            frameBuffer = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
        }

        frameBuffer.clear();
        frameBuffer.put(data);
        frameBuffer.rewind();
        frameAvailable = true;
    }

    private int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            String log = GLES20.glGetShaderInfoLog(shader);
            GLES20.glDeleteShader(shader);
            throw new RuntimeException("Shader compile failed: " + log);
        }
        return shader;
    }

    private int createProgram(String vertexCode, String fragmentCode) {
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentCode);
        int program = GLES20.glCreateProgram();

        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);

        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] != GLES20.GL_TRUE) {
            String log = GLES20.glGetProgramInfoLog(program);
            GLES20.glDeleteProgram(program);
            throw new RuntimeException("Program link failed: " + log);
        }
        return program;
    }
}
