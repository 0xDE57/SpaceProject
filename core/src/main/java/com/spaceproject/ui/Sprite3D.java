package com.spaceproject.ui;


import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;


//based on: xoppa.github.io/blog/a-simple-card-game/
public class Sprite3D extends Renderable implements Disposable {
    
    public final Vector3 position = new Vector3();
    public final Quaternion rotation = new Quaternion();
    public final Vector3 scale = new Vector3(1, 1, 1);
    public float angle;
    
    public Sprite3D(Texture frontTex, Texture backTex, float renderScale) {
        //combine textures: Sprites loaded into material texture must come from same texture
        int width = frontTex.getWidth(), height = frontTex.getHeight();
        Texture combinedTex = combineTextures(frontTex, backTex);
        
        //create 3D ship with front and back texture
        Sprite front = new Sprite(combinedTex, 0, 0, width, height);
        Sprite back = new Sprite(combinedTex, 0, height, width, height);
        
        material = new Material(
                TextureAttribute.createDiffuse(front.getTexture()),
                new BlendingAttribute(false, 1f)
                //,FloatAttribute.createAlphaTest(0.5f)
        );
        
        float scaledWidth = front.getWidth() * renderScale;
        float scaledHeight = front.getHeight() * renderScale;
        front.setSize(scaledWidth, scaledHeight);
        back.setSize(scaledWidth, scaledHeight);
        
        front.setPosition(-front.getWidth() * 0.5f, -front.getHeight() * 0.5f);
        back.setPosition(-back.getWidth() * 0.5f, -back.getHeight() * 0.5f);
        
        float[] vertices = convert(front.getVertices(), back.getVertices());
        short[] indices = new short[]{0, 1, 2, 2, 3, 0, 4, 5, 6, 6, 7, 4};
        
        meshPart.mesh = new Mesh(true, 8, 12, VertexAttribute.Position(), VertexAttribute.Normal(), VertexAttribute.TexCoords(0));
        meshPart.mesh.setVertices(vertices);
        meshPart.mesh.setIndices(indices);
        meshPart.offset = 0;
        meshPart.size = meshPart.mesh.getNumIndices();
        meshPart.primitiveType = GL20.GL_TRIANGLES;
        meshPart.update();
    }
    
    private Texture combineTextures(Texture textureA, Texture textureB) {
        Pixmap pixmap = new Pixmap(textureA.getWidth(), textureA.getHeight() * 2, Pixmap.Format.RGBA8888);
        pixmap.drawPixmap(textureA.getTextureData().consumePixmap(), 0, 0);
        pixmap.drawPixmap(textureB.getTextureData().consumePixmap(), 0, textureA.getHeight());
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }
    
    private static float[] convert(float[] front, float[] back) {
        return new float[]{
                front[Batch.X2], front[Batch.Y2], 0, 0, 0, 1, front[Batch.U2], front[Batch.V2],
                front[Batch.X1], front[Batch.Y1], 0, 0, 0, 1, front[Batch.U1], front[Batch.V1],
                front[Batch.X4], front[Batch.Y4], 0, 0, 0, 1, front[Batch.U4], front[Batch.V4],
                front[Batch.X3], front[Batch.Y3], 0, 0, 0, 1, front[Batch.U3], front[Batch.V3],
                
                back[Batch.X1], back[Batch.Y1], 0, 0, 0, -1, back[Batch.U1], back[Batch.V1],
                back[Batch.X2], back[Batch.Y2], 0, 0, 0, -1, back[Batch.U2], back[Batch.V2],
                back[Batch.X3], back[Batch.Y3], 0, 0, 0, -1, back[Batch.U3], back[Batch.V3],
                back[Batch.X4], back[Batch.Y4], 0, 0, 0, -1, back[Batch.U4], back[Batch.V4]
        };
    }
    
    public void update() {
        this.worldTransform.set(position, rotation, scale);
    }
    
    @Override
    public void dispose() {
        meshPart.mesh.dispose();
    }
}
