package com.patulus.huffmancoding.compressor;

import com.patulus.huffmancoding.general.Node;
import com.patulus.huffmancoding.minheap.MinHeap;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Compressor {
    /** 헤더 정보 구분을 위한 의미 없는 바이트를 추가합니다. */
    private static final int MEANINGLESS_CHARACTER = 255;

    /** 읽을 파일과 쓸 파일을 지정합니다. */
    final File src;
    final File out;

    /** 압축 전 파일의 문자 개수입니다. */
    int totalChars;
    /** 압축에 걸린 시간입니다. */
    double elapsedTime;

    /** 파일 압축 전 문자 수 및 문자별 출현 횟수를 세는 파일 읽기 클래스입니다. */
    private BufferedReader preprocessReader;
    /** 파일 압축을 위한 파일 읽기 클래스입니다. */
    private BufferedReader reader;
    /** 파일 압축을 위한 파일 쓰기 클래스입니다. */
    private BufferedOutputStream writer;
    /** (GUI) 압축된 파일의 내용을 반환합니다. */
    private StringBuilder compressResult;

    /** 허프만 트리를 구성하기 위한 최소 힙입니다. */
    private final MinHeap minHeap;
    /** 문자 출현 횟수를 저장합니다. */
    final Map<Integer, Integer> frequency;
    /** 허프만 코드를 저장합니다. */
    final Map<Character, String> huffmanCodes;

    public Compressor(String path) throws FileNotFoundException {
        this.src = new File(path);
        this.out = new File(src.getPath() + ".hfm");

        this.totalChars = 0;

        this.compressResult = new StringBuilder();

        this.minHeap = new MinHeap();
        this.frequency = new HashMap<>();
        this.huffmanCodes = new HashMap<>();

        init();
    }

    /** 스트림을 엽니다. */
    private void init() throws FileNotFoundException {
        try {
            preprocessReader = new BufferedReader(new InputStreamReader(new FileInputStream(src), StandardCharsets.UTF_8));
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(src), StandardCharsets.UTF_8));
            writer = new BufferedOutputStream(new FileOutputStream(out));
        } catch (FileNotFoundException ex) {
            System.err.println("파일을 찾을 수 없습니다: " + ex.getMessage());
            throw ex;
        }
    }

    /** 스트림을 닫습니다. */
    public void close() throws IOException {
        try {
            if (writer != null) writer.close();
            if (reader != null) reader.close();
            if (preprocessReader != null) preprocessReader.close();
        } catch (IOException ex) {
            System.err.println("해제 오류가 발생했습니다: " + ex.getMessage());
            throw ex;
        }
    }

    /** 압축을 수행합니다. */
    public void run() throws IOException {
        try {
            long startTime = System.nanoTime();

            // 문자별 출현 횟수를 세 허프만 트리를 구성합니다.
            makeHuffmanTree(calculateFrequency());
            // 노드 개수가 3 미만이면 부모 노드를 강제로 생성합니다.
            if (frequency.size() < 3) {
                Node left = minHeap.delete();
                Node right = minHeap.delete();
                if (right == null) {
                    right = new Node(MEANINGLESS_CHARACTER, 0, null, null);
                }

                minHeap.insert(new Node(MEANINGLESS_CHARACTER, left.getFrequency() + right.getFrequency(), left, right));
            }
            Node root = minHeap.delete();

            // 허프만 코드를 생성합니다.
            makeHuffmanCode(root, "");

            // 허프만 트리 정보를 파일에 씁니다. (0이면 내부 노드, 1이면 말단 노드)
            compressResult.append("[헤더]\n");
            writeHeader(root);
            // 문자를 읽고, 허프만 코드로 변환해 파일에 씁니다.
            compressResult.append("[본문]\n");
            writeBody();

            writer.flush();

            long endTime = System.nanoTime();

            elapsedTime = ((double) endTime - startTime) / 1000000;
            System.out.println(src.getName() + "의 압축 시간: " + elapsedTime);
        } catch (IOException ex) {
            System.err.println("압축 오류가 발생했습니다: " + ex.getMessage());
            throw ex;
        }
    }

    /** (테스트용!) 허프만 트리 헤더 정보를 점검합니다. */
    private void preorder(Node node) {
        if (node != null) {
            System.out.print((node.isLeaf()) ? 1 : 0);
            preorder(node.getLeft());
            preorder(node.getRight());
        }
    }

    /** (테스트용!) 허프만 트리 헤더 정보를 점검합니다. */
    private void preorderChar(Node node) {
        if (node != null) {
            if (node.isLeaf()) {
                System.out.print((char) node.getCharacter());
            }
            preorderChar(node.getLeft());
            preorderChar(node.getRight());
        }
    }

    /** 문자 출현 횟수를 계산합니다. */
    private List<Integer> calculateFrequency() throws IOException {
        int ch;
        
        try {
            // 문자 단위로 읽고, 문자 출현 횟수를 셉니다.
            while ((ch = preprocessReader.read()) != -1) {
                frequency.put(ch, frequency.getOrDefault(ch, 0) + 1);
                ++totalChars;
                if (totalChars == Integer.MAX_VALUE) {
                    throw new IOException("파일 용량이 너무 큽니다.");
                }
            }
        } catch (IOException ex) {
            System.err.println("전처리 오류가 발생했습니다: " + ex.getMessage());
            throw ex;
        }

        // 문자 출현 횟수순으로 정렬합니다.
        List<Integer> sortedChars = new ArrayList<>(frequency.keySet());
        sortedChars.sort((o1, o2) -> frequency.get(o2) - frequency.get(o1));

        return sortedChars;
    }

    /** 허프만 트리를 구성합니다. */
    public void makeHuffmanTree(List<Integer> sortedChars) throws IOException {
        if (sortedChars.isEmpty()) {
            throw new IOException("전처리 오류가 발생했습니다: 빈 텍스트 파일입니다.");
        }

        // 문자 출현 횟수순으로 최소 힙에 노드를 만들어 삽입합니다.
        for (int ch : sortedChars) {
            minHeap.insert(new Node(ch, frequency.get(ch)));
        }

        // 허프만 트리를 구성합니다.
        while (minHeap.size() > 1) {
            Node nodeA = minHeap.delete();
            Node nodeB = minHeap.delete();

            Node parentNode = new Node(MEANINGLESS_CHARACTER, nodeA.getFrequency() + nodeB.getFrequency(), nodeA, nodeB);
            minHeap.insert(parentNode);
        }
    }

    /** 허프만 코드를 생성합니다. */
    private void makeHuffmanCode(Node node, String code) {
        if (node != null) {
            if (node.isLeaf()) {
                huffmanCodes.put((char) node.getCharacter(), code);
            } else {
                makeHuffmanCode(node.getLeft(), code + "0");
                makeHuffmanCode(node.getRight(), code + "1");
            }
        }
    }

    /** 헤더 정보를 씁니다. */
    private void writeHeader(Node node) throws IOException {
        try {
            // 압축 파일의 식별자 'H'를 파일에 씁니다.
            writer.write('H');
            compressResult.append('H');
            compressResult.append("\n");

            // 읽은 문자의 수를 파일에 씁니다.
            for (int i = 4; i > 0; --i) {
                writer.write((totalChars >> (8 * (i - 1))) & 0xFF);
            }
            writer.write(MEANINGLESS_CHARACTER);
            compressResult.append(totalChars);
            compressResult.append("\n");

            // [0]은 1바이트의 수 값, [1]은 바이트에서 읽은 비트 index입니다.
            int[] info = {0, 0};
            // 노드 정보를 씁니다.
            writeHeaderNode(node, info);
            // 읽어야 하는 비트가 남았다면 파일에 씁니다.
            if (info[1] > 0) {
                info[0] <<= (8 - info[1]);
                writer.write(info[0]);
            }
            writer.write(MEANINGLESS_CHARACTER);
            compressResult.append("\n");

            // 말단 노드의 문자를 파일에 씁니다.
            writeHeaderCharacter(node);
            writer.write(MEANINGLESS_CHARACTER);
            compressResult.append("\n\n");
        } catch (IOException ex) {
            System.err.println("헤더 쓰기 중 오류 발생: " + ex.getMessage());
            throw ex;
        }
    }

    /** 트리 구조 정보를 파일에 씁니다. */
    private void writeHeaderNode(Node node, int[] info) throws IOException {
        if (node != null) {
            // 1바이트 이상이 되면 트리 구조 정보를 파일에 쓰고, 초기화합니다.
            if (info[1] >= 8) {
                writer.write(info[0]);
                compressResult.append(String.format("%08d", Integer.parseInt(Integer.toBinaryString(info[0]))));
                info[0] = 0;
                info[1] = 0;
            }
            // 트리 구조 정보를 파악합니다. (내부 노드는 0, 말단 노드는 1)
            info[0] = (info[0] << 1) | (node.isLeaf() ? 1 : 0);
            info[1]++;

            // 다음 노드로 이동합니다.
            writeHeaderNode(node.getLeft(), info);
            writeHeaderNode(node.getRight(), info);
        }
    }

    /** 트리의 문자 정보를 파일에 씁니다. */
    private void writeHeaderCharacter(Node node) throws IOException {
        if (node != null) {
            // 말단 노드이면 파일에 문자를 씁니다.
            if (node.isLeaf()) {
                writer.write(String.valueOf((char) node.getCharacter()).getBytes(StandardCharsets.UTF_8));
                compressResult.append("'").append((char) node.getCharacter()).append("' ");
                return;
            }

            // 내부 노드이면 말단 노드를 찾아 여정을 떠납니다.
            writeHeaderCharacter(node.getLeft());
            writeHeaderCharacter(node.getRight());
        }
    }

    /** 문자에 해당하는 허프만 코드를 파일에 씁니다. */
    private void writeBody() throws IOException {
        try {
            int readCh;
            int buffer = 0;
            int bufferIdx = 0;

            while ((readCh = reader.read()) != -1) {
                // 허프만 코드를 가져옵니다.
                String huffmanCode = huffmanCodes.get((char) readCh);

                if (huffmanCode == null) {
                    throw new IOException("허프만 코드가 존재하지 않습니다: " + (char) readCh);
                }

                // 문자열을 하나씩 읽어 비트에 씁니다.
                for (int codeIdx = 0; codeIdx < huffmanCode.length(); ++codeIdx) {
                    buffer = (buffer << 1) | (huffmanCode.charAt(codeIdx) - '0');
                    compressResult.append(buffer & 1);
                    bufferIdx++;

                    // 1바이트 이상이 되면 파일에 쓰고 초기화합니다.
                    if (bufferIdx >= 8) {
                        writer.write(buffer);
                        buffer = 0;
                        bufferIdx = 0;
                    }
                }
            }

            // 남은 비트를 0으로 채웁니다.
            if (bufferIdx > 0) {
                buffer <<= (8 - bufferIdx);
                writer.write(buffer);
            }
        } catch (IOException ex) {
            System.err.println("압축 오류가 발생했습니다: " + ex.getMessage());
            throw ex;
        }
    }

    /** (GUI) 압축한 결과를 반환합니다. */
    String getResult() { return this.compressResult.toString(); }
}
