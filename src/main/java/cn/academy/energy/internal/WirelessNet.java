/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * This project is open-source, and it is distributed under  
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * 本项目是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.energy.internal;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import cn.academy.core.Debug;
import cn.academy.energy.api.block.IWirelessMatrix;
import cn.academy.energy.api.block.IWirelessNode;
import cn.academy.energy.internal.VBlocks.VWMatrix;
import cn.academy.energy.internal.VBlocks.VWNode;

/**
 * @author WeAthFolD
 */
public class WirelessNet {
	
	static final int UPDATE_INTERVAL = 40;
	static final double BUFFER_MAX = 2000;

	final WiWorldData data;
	World world;
	
	List<VWNode> nodes = new LinkedList();
	
	VWMatrix matrix;
	
	String ssid;
	String password;
	
	double buffer;
	
	int aliveUpdateCounter = UPDATE_INTERVAL;
	
	boolean alive = false;
	boolean disposed = false;
	
	public WirelessNet(WiWorldData data, VWMatrix matrix, String ssid, String pass) {
		this.data = data;
		
		this.matrix = matrix;
		
		this.ssid = ssid;
		this.password = pass;
		
		System.out.println("Directly creating " + ssid);
	}
	
	public WirelessNet(WiWorldData data, NBTTagCompound tag) {
		this.data = data;
		
		//Load the matrix
		matrix = new VWMatrix(tag.getCompoundTag("matrix"));
		
		//Load the info
		ssid = tag.getString("ssid");
		password = tag.getString("password");
		buffer = tag.getDouble("buffer");
		
		//Load the node list
		NBTTagList list = (NBTTagList) tag.getTag("list");
		for(int i = 0; i < list.tagCount(); ++i) {
			doAddNode(new VWNode(list.getCompoundTagAt(i)));
		}
		
		Debug.print("Loading " + ssid + " from NBT, " + list.tagCount() + " nodes.");
	}
	
	NBTTagCompound toNBT() { 
		NBTTagCompound tag = new NBTTagCompound();
		tag.setTag("matrix", matrix.toNBT());
		tag.setString("ssid", ssid);
		tag.setString("password", password);
		tag.setDouble("buffer", buffer);
		
		NBTTagList list = new NBTTagList();
		for(VWNode vn : nodes) {
			if(!vn.isLoaded(world) || vn.get(world) != null) {
				list.appendTag(vn.toNBT());
			}
		}
		tag.setTag("list", list);
		
		Debug.print(ssid + " toNBT()");
		
		return tag; 
	}
	
	public String getSSID() {
		return ssid;
	}
	
	public String getPassword() {
		return password;
	}
	
	public boolean resetPassword(String p, String np) {
		if(!p.equals(password))
			return false;
		password = np;
		return true;
	}
	
	/**
	 * Get whether this matrix is alive (That is, there are >=1 node loaded and should be ticked normally).
	 */
	public boolean isAlive() {
		return alive;
	}
	
	public boolean isDisposed() {
		return disposed;
	}
	
	/**
	 * Dispose (a.k.a. destroy) this network and unlink all its linked nodes.
	 */
	void dispose() {
		disposed = true;
	}
	
	boolean addNode(VWNode node, String password) {
		
		if(!password.equals(this.password))
			return false;
		
		WiWorldData data = getWorldData();
		
		//Check if this node is previously added
		WirelessNet other = data.getNetwork(node.get(world));
		if(other != null) {
			other.removeNode(node);
		}
		
		doAddNode(node);
		
		return true;
	}
	
	private void doAddNode(VWNode node) {
		//Really add
		WiWorldData data = getWorldData();
		nodes.add(node);
		data.networks.put(node, this);
	}
	
	void removeNode(VWNode node) {
		nodes.remove(node);
		
		WiWorldData data = getWorldData();
		data.networks.remove(node);
	}
	
	void onCreate(WiWorldData data) {
		data.networks.put(ssid, this);
		data.networks.put(matrix, this);
	}
	
	void onCleanup(WiWorldData data) {
		data.networks.remove(ssid);
		data.networks.remove(matrix);
		
		for(VWNode n : nodes) {
			data.networks.remove(n);
		}
	}
	
	private WiWorldData getWorldData() {
		return data;
	}
	
	/**
	 * This is a slightly costy function. You should buffer the result and query through isAlive().
	 * query it infrequently.
	 * @return same as isAlive
	 */
	private boolean checkIsAlive() {
		//IF: alive node count > 1
		for(VWNode node : nodes) {
			if(node.isLoaded(world) && node.get(world) != null) {
				alive = true;
				return true;
			}
		}
		alive = false;
		return false;
	}
	
	void tick() {
		
		// Check whether the matrix is valid. The matrix is ALWAYS loaded.
		IWirelessMatrix imat = matrix.get(world);
		if(imat == null) {
			Debug.print("WirelessNet with SSID " + ssid + " matrix destoryed, removing");
			dispose();
			return;
		}
		
		// Filter the not-alive nets and update the state lazily
		if(!isAlive()) {
			--aliveUpdateCounter;
			if(aliveUpdateCounter == 0) {
				aliveUpdateCounter = UPDATE_INTERVAL;
				checkIsAlive();
			}
			
			return;
		}
		
		// Balance.
		double sum = buffer, maxSum = BUFFER_MAX;
		Iterator<VWNode> iter = nodes.iterator();
		while(iter.hasNext()) {
			VWNode vn = iter.next();
			if(vn.isLoaded(world)) {
				IWirelessNode node = vn.get(world);
				if(node == null) {
					Debug.print("Removing " + vn + " from " + ssid);
					iter.remove();
				} else {
					sum += node.getEnergy();
					maxSum += node.getMaxEnergy();
				}
			}
		}
		
		double percent = sum / maxSum;
		double transferLeft = imat.getLatency();
		// Loop through and calc
		for(VWNode vn : nodes) {
			if(vn.isLoaded(world)) {
				IWirelessNode node = vn.get(world);
				
				double cur = node.getEnergy();
				double targ = node.getMaxEnergy() * percent;
				
				double delta = targ - cur;
				delta = Math.signum(delta) * Math.min(Math.abs(delta), Math.min(transferLeft, node.getLatency()));
				
				if(buffer + delta > BUFFER_MAX) {
					delta = BUFFER_MAX - buffer;
				} else if(buffer + delta < 0) {
					delta = -buffer;
				}
				
				transferLeft -= Math.abs(delta);
				buffer += delta;
				node.setEnergy(cur + delta);
				
				if(transferLeft == 0)
					break;
			}
		}
	}
	
}