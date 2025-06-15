package com.mengnankk.mydatabase.backend.im;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

public class NodeCache {
    private final int capacity;
    private int minFreq;
    private final Map<Long, Node> nodeMap;//key -> Node
    private final Map<Long, Integer> freqMap; // key -> freq
    private final Map<Integer, LinkedHashSet<Long>> freqList;// freq -> keys

    public NodeCache(int capacity, Map<Long, Node> nodeMap, Map<Long, Integer> freqMap, Map<Integer, LinkedHashSet<Long>> freqList) {
        this.capacity = capacity;
        this.nodeMap = new HashMap<>();
        this.freqMap = new HashMap<>();
        this.freqList = new HashMap<>();
        this.minFreq = 1;
    }

    public NodeCache(int capacity) {
        this.capacity= capacity;
        this.nodeMap = new HashMap<>();
        this.freqMap = new HashMap<>();
        this.freqList = new HashMap<>();
        this.minFreq = 1;
    }


    /**
     * get
     * @param id
     * @return
     */
    public synchronized Node get(long id){
        if (!nodeMap.containsKey(id)) return null;
        int freq = freqMap.get(id);
        LinkedHashSet<Long> oldSet = freqList.get(freq);
        oldSet.remove(id);
        if (oldSet.isEmpty()){
            freqList.remove(freq);
            if (freq==minFreq){
                minFreq++;
            }
        }
        freqList.computeIfAbsent(freq+1,ignore->new LinkedHashSet<>()).add(id);
        return nodeMap.get(id);
    }
    public synchronized void put(Long id,Node node){
        if(capacity==0) return;
        if (nodeMap.containsKey(id)){
            nodeMap.put(id,node);
            get(id);
            return;
        }
        if (nodeMap.size()>=capacity){
            LinkedHashSet<Long> minFreqKeys =freqList.get(minFreq);
            long evicId= minFreqKeys.iterator().next();
            if (minFreqKeys.isEmpty()){
                freqList.remove(minFreq);
            }
            nodeMap.remove(evicId);
            freqMap.remove(evicId);
        }
        nodeMap.put(id,node);
        freqMap.put(id,1);
        freqList.computeIfAbsent(1, ignore -> new LinkedHashSet<>()).add(id);
        minFreq = 1;
    }
    public synchronized void clear(){
        nodeMap.clear();
        freqMap.clear();
        freqList.clear();
        minFreq = 0;
    }
}
