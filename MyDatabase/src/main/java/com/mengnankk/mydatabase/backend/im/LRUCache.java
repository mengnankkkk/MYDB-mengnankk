package com.mengnankk.mydatabase.backend.im;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.*;

public class LRUCache<K, V> {
    private final int capacity;
    private final ConcurrentHashMap<K, Node<K, V>> map;
    private final DoublyLinkedList<K, V> list;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public LRUCache(int capacity) {
        this.capacity = capacity;
        this.map = new ConcurrentHashMap<>(capacity);
        this.list = new DoublyLinkedList<>();
    }

    public V get(K key) {
        lock.readLock().lock();
        try {
            Node<K, V> node = map.get(key);
            if (node == null) return null;
            // 提升到写锁移动节点
            lock.readLock().unlock();
            lock.writeLock().lock();
            try {
                list.moveToFront(node);
            } finally {
                lock.readLock().lock(); // 降级
                lock.writeLock().unlock();
            }
            return node.value;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void put(K key, V value) {
        lock.writeLock().lock();
        try {
            Node<K, V> node = map.get(key);
            if (node != null) {
                node.value = value;
                list.moveToFront(node);
            } else {
                Node<K, V> newNode = new Node<>(key, value);
                map.put(key, newNode);
                list.addFirst(newNode);
                if (map.size() > capacity) {
                    Node<K, V> removed = list.removeLast();
                    if (removed != null) {
                        map.remove(removed.key);
                    }
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void remove(K key) {
        lock.writeLock().lock();
        try {
            Node<K, V> node = map.remove(key);
            if (node != null) {
                list.remove(node);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    // ================================
    // 内部数据结构
    // ================================
    private static class Node<K, V> {
        final K key;
        volatile V value;
        Node<K, V> prev;
        Node<K, V> next;

        Node(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }

    private static class DoublyLinkedList<K, V> {
        private final Node<K, V> head;
        private final Node<K, V> tail;

        DoublyLinkedList() {
            head = new Node<>(null, null);
            tail = new Node<>(null, null);
            head.next = tail;
            tail.prev = head;
        }

        void addFirst(Node<K, V> node) {
            node.next = head.next;
            node.prev = head;
            head.next.prev = node;
            head.next = node;
        }

        void remove(Node<K, V> node) {
            node.prev.next = node.next;
            node.next.prev = node.prev;
        }

        void moveToFront(Node<K, V> node) {
            remove(node);
            addFirst(node);
        }

        Node<K, V> removeLast() {
            if (tail.prev == head) return null;
            Node<K, V> last = tail.prev;
            remove(last);
            return last;
        }
    }
}
