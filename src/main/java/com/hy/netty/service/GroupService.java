package com.hy.netty.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class GroupService {
	
	private static Map<String, Set<String>> userId2GroupIds = new ConcurrentHashMap<>();
	
	public static Map<String, Set<String>> initUser2GroupIds(){
		
		Set<String> groupIds=new HashSet<String>();
		groupIds.add("G1");
		groupIds.add("G2");
		userId2GroupIds.put("111", groupIds);
		groupIds=new HashSet<String>();
		groupIds.add("G2");
		groupIds.add("G3");
		userId2GroupIds.put("222", groupIds);
		groupIds=new HashSet<String>();
		groupIds.add("G1");
		groupIds.add("G3");
		userId2GroupIds.put("333", groupIds);
		
		return userId2GroupIds;
	}
	
	public static String findUserIdByUserToken(String userToken){
		Map<String,String> userToken2UserId=new HashMap<String,String>();
		userToken2UserId.put("aaa", "111");
		userToken2UserId.put("bbb", "222");
		userToken2UserId.put("ccc", "333");
		return userToken2UserId.get(userToken);
	}

}
