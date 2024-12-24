package com.patulus.huffmancoding.general;

public class Node {
    private int character;
    private int frequency;
    private Node left, right;

    public Node(int character, int frequency) {
        this(character, frequency, null, null);
    }

    public Node(int character, int frequency, Node left, Node right) {
        this.character = character;
        this.frequency = frequency;
        this.left = left;
        this.right = right;
    }

    public int getCharacter() {
        return this.character;
    }

    public void setCharacter(int ch) { this.character = (char)ch; }

    public int getFrequency() {
        return this.frequency;
    }

    public Node getLeft() {
        return this.left;
    }

    public Node getRight() {
        return this.right;
    }

    public boolean isLeaf() { return this.left == null && this.right == null; }
}
