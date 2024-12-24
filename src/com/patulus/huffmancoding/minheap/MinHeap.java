package com.patulus.huffmancoding.minheap;

import com.patulus.huffmancoding.general.Node;

import java.util.Arrays;

public class MinHeap {
    private int capacity = 512;

    private Node[] heap;
    private int heapSize;

    public MinHeap() {
        this.heap = new Node[capacity];
        this.heapSize = 0;
    }

    public void insert(Node node) {
        int newSize = ++heapSize;
        if (heapSize >= capacity) {
            capacity *= 2;
            heap = Arrays.copyOf(heap, capacity);
        }

        // 삽입 노드의 비용이 부모 노드의 비용보다 낮으면 부모 노드를 자식 노드로 이동시킵니다.
        // 최소 힙의 부모 - 자식 관계를 맞춰지도록 루트 노드까지 반복합니다.
        while (newSize != 1 && heap[newSize / 2].getFrequency() > node.getFrequency()) {
            heap[newSize] = heap[newSize / 2];
            newSize /= 2;
        }
        // 최종 위치에 삽입 노드를 삽입합니다.
        heap[newSize] = node;
    }

    public Node delete() {
        if (heapSize <= 0) {
            return null;
        }

        // 삭제 노드인 루트 노드를 일시 저장하고, 최하단부 노드를 루트 노드로 이동시킵니다.
        Node delNode = heap[1];
        Node lastNode = heap[heapSize--];

        int parent = 1;
        int child = 2;

        while (child <= heapSize) {
            // 최소 힙의 부모 - 자식 관계가 맞춰지도록 부모 노드로 이동시킬 자식 노드를 선택합니다.
            // 처음 단계에서 왼쪽 자식보다 오른쪽 자식이 더 작으면(이동에 적합하면) 오른쪽 노드를 선택합니다.
            if (child < heapSize && heap[child].getFrequency() > heap[child + 1].getFrequency()) {
                ++child;
            }

            // 최소 힙의 부모 - 자식 관계를 만족하면 중단합니다.
            if (lastNode.getFrequency() < heap[child].getFrequency()) {
                break;
            }

            // 최소 힙의 부모 - 자식 관계를 만족시키도록 노드를 이동시킵니다.
            heap[parent] = heap[child];
            parent = child;
            child *= 2;
        }
        // 최종 위치에 최하단부 노드를 위치시킵니다.
        // 해당 위치가 최소 힙의 부모 - 자식 관계가 맞춰지는 위치가 됩니다.
        heap[parent] = lastNode;

        // 삭제 노드를 반환합니다.
        return delNode;
    }

    public int size() {
        return this.heapSize;
    }
}
