package net.preibisch.distribution.algorithm.controllers.items;

import java.io.IOException;

import net.preibisch.distribution.algorithm.controllers.items.callback.AbstractCallBack;

public interface AbstractTask {
	
	public void start(int pos,AbstractCallBack callback)throws Exception;
}
