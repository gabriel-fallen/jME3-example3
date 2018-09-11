/*
Copyright 2018 Alexander Tchitchigin.
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package mygame;

import com.jme3.app.SimpleApplication;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Plane;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;
import com.jme3.terrain.geomipmap.TerrainLodControl;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.heightmap.AbstractHeightMap;
import com.jme3.terrain.heightmap.ImageBasedHeightMap;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.util.SkyFactory;
import com.jme3.water.SimpleWaterProcessor;
import com.jme3.water.WaterFilter;

/**
 * This is the Main Class of your Game. You should only do initialization here.
 * Move your Logic into AppStates or Controls
 * @author normenhansen
 */
public class Main extends SimpleApplication {

    public static void main(String[] args) {
        Main app = new Main();
        app.start();
    }
    
    private TerrainQuad terrain;
    private Material mat_terrain;
    private final Vector3f lightDir = new Vector3f(4.9f, -4.3f, -5.9f); // "Sun" for the scene and for the "ocean"

    @Override
    public void simpleInitApp() {
        cam.setLocation(new Vector3f(100, -80, -300));
        flyCam.setMoveSpeed(50);

        /** 1. Create terrain material and load four textures into it. */
        mat_terrain = new Material(assetManager, "Common/MatDefs/Terrain/Terrain.j3md");

        /** 1.1) Add ALPHA map (for red-blue-green coded splat textures) */
        mat_terrain.setTexture("Alpha", assetManager.loadTexture("Textures/Terrain/splat/alphamap.png"));

        /** 1.2) Add GRASS texture into the red layer (Tex1). */
        Texture grass = assetManager.loadTexture("Textures/Terrain/splat/grass.jpg");
        grass.setWrap(WrapMode.Repeat);
        mat_terrain.setTexture("Tex1", grass);
        mat_terrain.setFloat("Tex1Scale", 64f);

        /** 1.3) Add DIRT texture into the green layer (Tex2) */
        Texture dirt = assetManager.loadTexture("Textures/Terrain/splat/dirt.jpg");
        dirt.setWrap(WrapMode.Repeat);
        mat_terrain.setTexture("Tex2", dirt);
        mat_terrain.setFloat("Tex2Scale", 32f);

        /** 1.4) Add ROAD texture into the blue layer (Tex3) */
        Texture rock = assetManager.loadTexture("Textures/Terrain/splat/road.jpg");
        rock.setWrap(WrapMode.Repeat);
        mat_terrain.setTexture("Tex3", rock);
        mat_terrain.setFloat("Tex3Scale", 128f);

        /** 2. Create the height map */
        Texture heightMapImage = assetManager.loadTexture("Textures/Terrain/splat/mountains512.png");
        AbstractHeightMap heightmap = new ImageBasedHeightMap(heightMapImage.getImage());
        heightmap.load();
        
        final int patchSize = 65;
        terrain = new TerrainQuad("my terrain", patchSize, 513, heightmap.getHeightMap());

        /** 4. We give the terrain its material, position & scale it, and attach it. */
        terrain.setMaterial(mat_terrain);
        terrain.setLocalTranslation(0, -100, 0);
        terrain.setLocalScale(2f, 1f, 2f);
        rootNode.attachChild(terrain);

        /** 5. The LOD (level of detail) depends on were the camera is: */
        TerrainLodControl control = new TerrainLodControl(terrain, getCamera());
        terrain.addControl(control);
        
        // The sky
        Spatial sky = SkyFactory.createSky(getAssetManager(), "Textures/Sky/Bright/BrightSky.dds", SkyFactory.EnvMapType.CubeMap);
        rootNode.attachChild(sky);
        
        DirectionalLight sun = new DirectionalLight(lightDir, ColorRGBA.White.mult(1.3f));
        rootNode.addLight(sun);
        
        makeFire();
        setupWater();
        setupOcean();
    }
    
    private void setupWater() {
        // we create a water processor
        SimpleWaterProcessor waterProcessor = new SimpleWaterProcessor(assetManager);
        waterProcessor.setReflectionScene(terrain);
//        waterProcessor.setLightPosition(new Vector3f(-500, 100, 500));

        final float waterY = -80;
        // we set the water plane
        Vector3f waterLocation = new Vector3f(0, waterY, 0);
        waterProcessor.setPlane(new Plane(Vector3f.UNIT_Y, waterLocation.dot(Vector3f.UNIT_Y)));
        viewPort.addProcessor(waterProcessor);

        // we set wave properties
        waterProcessor.setWaterDepth(20);         // transparency of water
        waterProcessor.setDistortionScale(0.05f); // strength of waves
        waterProcessor.setWaveSpeed(0.05f);       // speed of waves

        // we define the wave size by setting the size of the texture coordinates
        Quad quad = new Quad(250, 250);
        quad.scaleTextureCoordinates(new Vector2f(6f,6f));

        // we create the water geometry from the quad
        Geometry water = new Geometry("water", quad);
        water.setLocalRotation(new Quaternion().fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_X));
        water.setLocalTranslation(-500, waterY, 450);
        water.setShadowMode(ShadowMode.Receive);
        water.setMaterial(waterProcessor.getMaterial());
        rootNode.attachChild(water);
    }
    
    private FilterPostProcessor fpp;
    private WaterFilter ocean;
    private final float initialWaterHeight = -90f;
    
    private void setupOcean() {
        fpp = new FilterPostProcessor(assetManager);
        ocean = new WaterFilter(rootNode, lightDir);
        ocean.setWaterHeight(initialWaterHeight);
        fpp.addFilter(ocean);
        viewPort.addProcessor(fpp);
    }
    
    private void makeFire() {
        ParticleEmitter fire = new ParticleEmitter("Fire", ParticleMesh.Type.Triangle, 80);
        Material mat_red = new Material(assetManager, "Common/MatDefs/Misc/Particle.j3md");
        mat_red.setTexture("Texture", assetManager.loadTexture("Effects/Explosion/flame.png"));
        fire.setMaterial(mat_red);
        fire.setImagesX(2);
        fire.setImagesY(2); // 2x2 texture animation
        fire.setEndColor(  new ColorRGBA(1f, 0f, 0f, 1f));   // red
        fire.setStartColor(new ColorRGBA(1f, 1f, 0f, 0.5f)); // yellow
        fire.getParticleInfluencer().setInitialVelocity(new Vector3f(0, 2, 0));
        fire.setStartSize(2.5f);
        fire.setEndSize(0.1f);
        fire.setGravity(0, 0, 0);
        fire.setLowLife(1f);
        fire.setHighLife(4f);
        fire.getParticleInfluencer().setVelocityVariation(0.3f);
        fire.setLocalTranslation(200, -55, -400);
        rootNode.attachChild(fire);
        
        ParticleEmitter smoke = new ParticleEmitter("Smoke", ParticleMesh.Type.Triangle, 3500);
        Material mat_smoke = new Material(assetManager, "Common/MatDefs/Misc/Particle.j3md");
        mat_smoke.setTexture("Texture", assetManager.loadTexture("Effects/Smoke/Smoke.png"));
        smoke.setMaterial(mat_smoke);
        smoke.setImagesX(15);
        smoke.setEndColor(  new ColorRGBA(0.7f, 0.7f, 0.7f, 0.7f));
        smoke.setStartColor(new ColorRGBA(0.001f, 0.001f, 0.001f, 1f));
        smoke.getParticleInfluencer().setInitialVelocity(new Vector3f(0, 4, 0));
        smoke.setGravity(0, 0, 0);
        smoke.setLowLife(5f);
        smoke.setHighLife(10f);
        smoke.getParticleInfluencer().setVelocityVariation(0.2f);
        smoke.setLocalTranslation(200, -55, -400);
        rootNode.attachChild(smoke);
    }

    @Override
    public void simpleUpdate(float tpf) {
        //TODO: add update code
    }

    @Override
    public void simpleRender(RenderManager rm) {
        //TODO: add render code
    }
}
