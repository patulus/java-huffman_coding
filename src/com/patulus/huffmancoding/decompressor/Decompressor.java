package com.patulus.huffmancoding.decompressor;

import com.patulus.huffmancoding.general.Node;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

public class Decompressor {
    /** 헤더 정보 구분을 위한 의미 없는 바이트를 추가합니다. */
    private static final int MEANINGLESS_CHARACTER = 255;

    /** 읽을 파일과 쓸 파일을 지정합니다. */
    final File src;
    final File out;

    /** 압축 전 파일의 문자 개수입니다. */
    int totalChars;
    /** 압축 해제에 걸린 시간입니다. */
    double elapsedTime;

    /** 파일 압축을 위한 파일 읽기 클래스입니다. */
    private BufferedInputStream reader;
    /** 파일 압축을 위한 파일 쓰기 클래스입니다. */
    private BufferedWriter writer;
    /** (GUI) 압축 해제된 파일의 내용을 반환합니다. */
    private StringBuilder decompressResult;

    /** 헤더 읽기를 위한 버퍼입니다. */
    private int readInfo;
    private int infoIdx;

    /** 허프만 트리의 루트 노드입니다. */
    Node root;

    public Decompressor(String path) throws FileNotFoundException {
        this.src = new File(path);
        this.out = new File(src.getPath().replace(".hfm", "-decompressed.txt"));

        this.totalChars = 0;

        this.decompressResult = new StringBuilder();

        this.readInfo = 0;
        this.infoIdx = 0;

        init();
    }

    /** 스트림을 엽니다. */
    private void init() throws FileNotFoundException {
        try {
            this.reader = new BufferedInputStream(new FileInputStream(this.src));
            this.writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(this.out), StandardCharsets.UTF_8));
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
        } catch (IOException ex) {
            System.err.println("해제 오류가 발생했습니다: " + ex.getMessage());
            throw ex;
        }
    }

    /** 압축 해제를 수행합니다. */
    public void run() throws IOException {
        try {
            long startTime = System.nanoTime();

            // 식별자를 확인합니다.
            int ch = reader.read();
            if (ch != 'H') {
                throw new IOException("압축된 파일이 아니거나 손상되었습니다.");
            }

            // 허프만 트리를 재구성합니다.
            readHeader();

            // 허프만 코드를 읽고, 문자로 변환해 파일로 씁니다.
            readBody();

            writer.flush();

            long endTime = System.nanoTime();

            elapsedTime = ((double) endTime - startTime) / 1000000;
            System.out.println(src.getName() + "의 복원 시간: " + elapsedTime);
        } catch (IOException ex) {
            System.err.println("압축 해제 중 오류 발생: " + ex.getMessage());
        }
    }

    /** (테스트용!) 허프만 트리 헤더 정보를 점검합니다. */
    private void preorder(Node node) {
        if (node != null) {
            System.out.print(node.isLeaf() ? 1 : 0);
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

    /** 헤더 정보를 읽습니다. */
    private void readHeader() throws IOException {
        try {
            // 읽은 문자의 수를 파일에서 불러옵니다.
            for (int i = 4; i > 0; --i) {
                int byteRead = reader.read();
                if (byteRead == -1) {
                    throw new IOException("파일의 끝에 도달했습니다.");
                }
                totalChars += byteRead << (8 * (i - 1));
            }
            if (reader.read() != MEANINGLESS_CHARACTER) {
                throw new IOException("파일 형식이 올바르지 않습니다.");
            }

            // 허프만 트리를 재구성합니다.
            root = readTreeNodeInfo();
            if (reader.read() != MEANINGLESS_CHARACTER) {
                throw new IOException("파일 형식이 올바르지 않습니다.");
            }

            // 허프만 트리의 말단 노드의 문자 정보를 변경합니다.
            readTreeCharInfo(root);
            if (reader.read() != MEANINGLESS_CHARACTER) {
                throw new IOException("파일 형식이 올바르지 않습니다.");
            }
        } catch (IOException ex) {
            System.err.println("헤더 읽기 중 오류 발생: " + ex.getMessage());
        }
    }

    /** 트리 구조 정보를 불러와 허프만 트리를 재구성합니다. */
    private Node readTreeNodeInfo() throws IOException {
        // 비트 하나를 가져옵니다.
        int bit = readInfoBit();
        if (bit == -1) return null;

        // 노드를 생성합니다.
        if (bit == 0) {
            Node leftChild = readTreeNodeInfo();
            Node rightChild = readTreeNodeInfo();
            return new Node(MEANINGLESS_CHARACTER, -1, leftChild, rightChild);
        } else {
            return new Node(MEANINGLESS_CHARACTER, -1, null, null);
        }
    }

    /** 트리의 문자 정보를 불러와 말단 노드의 문자 정보를 갱신합니다. */
    private void readTreeCharInfo(Node node) throws IOException {
        if (node != null) {
            // 말단 노드이면 노드를 갱신합니다.
            if (node.isLeaf()) {
                // 바이트 단위로 가져옵니다.
                int firstByte = reader.read();
                if (firstByte == -1) {
                    throw new IOException("파일의 끝에 도달했습니다.");
                }
                if (firstByte == MEANINGLESS_CHARACTER) return;

                // UTF-8 지원을 위해 추가로 읽을 바이트를 확인합니다.
                int numBytes;
                // 0xxxxxxx
                if ((firstByte & 0x80) == 0) {
                    numBytes = 1;
                // 110xxxxx 10xxxxxx
                // 앞 세 비트가 110이면 1바이트를 더 읽습니다.
                } else if ((firstByte & 0xE0) == 0xC0) {
                    numBytes = 2;
                // 1110xxxx 10xxxxxx 10xxxxxx
                // 앞 네 비트가 1110이면 2바이트를 더 읽습니다.
                } else if ((firstByte & 0xF0) == 0xE0) {
                    numBytes = 3;
                // 11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
                // 앞 다섯 비트가 11110이면 3바이트를 더 읽습니다.
                } else if ((firstByte & 0xF8) == 0xF0) {
                    numBytes = 4;
                } else {
                    throw new IOException("잘못된 UTF-8 인코딩입니다.");
                }

                byte[] charBytes = new byte[numBytes];
                charBytes[0] = (byte) firstByte;

                for (int i = 1; i < numBytes; i++) {
                    int nextByte = reader.read();
                    if (nextByte == -1) {
                        throw new IOException("파일의 끝에 도달했습니다.");
                    }
                    charBytes[i] = (byte) nextByte;
                }

                // 읽은 바이트를 하나의 문자로 변환 후 노드를 갱신합니다.
                String character = new String(charBytes, StandardCharsets.UTF_8);
                node.setCharacter(character.charAt(0));
            // 말단 노드를 찾아 여정을 떠납니다...
            } else {
                readTreeCharInfo(node.getLeft());
                readTreeCharInfo(node.getRight());
            }
        }
    }

    /** 바이트 단위로 읽고 비트 하나를 반환합니다. */
    private int readInfoBit() throws IOException {
        if (infoIdx == 0) {
            readInfo = reader.read();
            if (readInfo == -1 || readInfo == MEANINGLESS_CHARACTER) {
                return -1;
            }
            infoIdx = 8;
        }
        --infoIdx;
        return (readInfo >> infoIdx) & 1;
    }

    /** 허프만 코드에 해당하는 문자를 읽어 파일에 씁니다. */
    private void readBody() {
        try {
            Node node = root;
            int readChars = 0;
            int readByte;

            while (readChars < totalChars && (readByte = reader.read()) != -1) {
                for (int bitIdx = 7; bitIdx >= 0; --bitIdx) {
                    // 비트를 읽습니다.
                    int bit = (readByte >> bitIdx) & 1;
                    // 0이면 왼쪽, 1이면 오른쪽 노드를 탐색합니다.
                    node = (bit == 0) ? node.getLeft() : node.getRight();

                    if (node == null) {
                        throw new IOException("트리 탐색 중 오류 발생: 노드가 null입니다.");
                    }

                    // 말단 노드에 도착하면 파일에 씁니다.
                    if (readChars <= totalChars && node.isLeaf()) {
                        writer.write(node.getCharacter());
                        decompressResult.append((char) node.getCharacter());
                        ++readChars;

                        if (readChars >= totalChars) {
                            break;
                        }

                        // 노드를 루트 노드로 초기화하고 다음 허프만 코드를 읽습니다.
                        node = root;
                    }
                }
            }
        } catch (IOException ex) {
            System.err.println("본문 읽기 중 오류 발생: " + ex.getMessage());
        }
    }

    public String getResult() { return this.decompressResult.toString(); }
}
