/*
 * Copyright (C) 2021 Jacob Wysko
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package org.wysko.midis2jam2;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.post.SceneProcessor;
import com.jme3.profile.AppProfiler;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.texture.FrameBuffer;

public class WireProcessor implements SceneProcessor {
	
	RenderManager renderManager;
	
	Material wireMaterial;
	
	public WireProcessor(AssetManager assetManager) {
		wireMaterial = new Material(assetManager, "/Common/MatDefs/Misc/Unshaded.j3md");
		wireMaterial.setColor("Color", ColorRGBA.LightGray);
		wireMaterial.getAdditionalRenderState().setWireframe(true);
	}
	
	@Override
	public void initialize(RenderManager rm, ViewPort vp) {
		renderManager = rm;
	}
	
	@Override
	public void reshape(ViewPort vp, int w, int h) {
		throw new UnsupportedOperationException("Not supported yet.");
	}
	
	@Override
	public boolean isInitialized() {
		return renderManager != null;
	}
	
	@Override
	public void preFrame(float tpf) {
	}
	
	@Override
	public void postQueue(RenderQueue rq) {
		renderManager.setForcedMaterial(wireMaterial);
	}
	
	@Override
	public void postFrame(FrameBuffer out) {
		renderManager.setForcedMaterial(null);
	}
	
	@Override
	public void cleanup() {
		renderManager.setForcedMaterial(null);
	}
	
	@Override
	public void setProfiler(AppProfiler profiler) {
	
	}
}