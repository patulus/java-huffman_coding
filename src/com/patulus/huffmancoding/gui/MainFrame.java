package com.patulus.huffmancoding.gui;

import com.patulus.huffmancoding.compressor.Compressor;
import com.patulus.huffmancoding.compressor.CompressorData;
import com.patulus.huffmancoding.decompressor.Decompressor;
import com.patulus.huffmancoding.decompressor.DecompressorData;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class MainFrame extends JFrame implements DropTargetListener {
    private final JFileChooser fileChooser;

    private File file;

    private static final String APPLICATION_TITLE = "Text Compressor v0.0.1 (Made by PARK, Yeonjong / 20210463 / kit CE)";
    private static final int DEFAULT_WIDTH = 600;
    private static final int DEFAULT_HEIGHT = 500;

    JPanel panel;
    JTextArea resultArea;
    JTextArea frequencyArea;
    JTextArea volumeArea;
    JButton button;

    public MainFrame() {
        this.fileChooser = new JFileChooser(System.getProperty("user.dir"));

        // 타이틀을 지정하고, 우측 상단의 X 버튼을 누르면 프로그램이 종료되도록 설정합니다.
        setTitle(APPLICATION_TITLE);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // 그리드 백 레이아웃으로 그리드 셀 하나에 하나의 컴포넌트가 아닌,
        // 여러 개의 셀에 하나의 컴포넌트를 사용할 수 있습니다.
        panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // 압축 결과 또는 압축 해제 결과를 표시하는 텍스트 에리어 컴포넌트입니다.
        resultArea = new JTextArea();
        resultArea.setEditable(false);
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);
        // 문자 출현 횟수를 표시하는 텍스트 에리어 컴포넌트입니다.
        frequencyArea = new JTextArea();
        frequencyArea.setEditable(false);
        frequencyArea.setLineWrap(true);
        frequencyArea.setWrapStyleWord(true);
        // 압축 또는 복원 전후의 용량을 표시하는 텍스트 에리어 컴포넌트입니다.
        volumeArea = new JTextArea();
        volumeArea.setEditable(false);
        volumeArea.setLineWrap(true);
        volumeArea.setWrapStyleWord(true);
        // 파일을 불러와 압축 또는 복원을 진행하는 버튼 컴포넌트입니다.
        button = new JButton("불러오기 및 압축/복원해 저장");
        ButtonActionListener buttonActionListener = new ButtonActionListener();
        button.addActionListener(buttonActionListener);

        DropTarget dropTarget = new DropTarget(this, this);
        setDropTarget(dropTarget);
        panel.setDropTarget(dropTarget);

        // 그리드 백 레이아웃 객체 내부에서 컴포넌트를 배치하는 방법을 지정하는 그리드 백 콘스트레인트를 생성합니다.
        GridBagConstraints gbc = new GridBagConstraints();
        // 컴포넌트 크기가 컨테이너보다 크면 가로 및 세로를 모두 확장시켜 공간을 채우도록 설정합니다.
        gbc.fill = GridBagConstraints.BOTH;

        // 왼쪽 영역에는 압축 또는 복원 결과를 표시합니다.
        // 셀 (0, 0)
        gbc.gridx = 0;
        gbc.gridy = 0;
        // 세로로 셀 2개를 차지하도록 설정합니다.
        gbc.gridheight = 2;
        // 가로로 70%를 차지합니다.
        gbc.weightx = 0.7;
        // 세로는 전체를 차지합니다.
        gbc.weighty = 1.0;
        panel.add(new JScrollPane(resultArea), gbc);

        // 오른쪽 상단 영역에는 문자 출현 횟수를 표시합니다.
        // 셀 (1, 0)
        gbc.gridx = 1;
        gbc.gridy = 0;
        // 세로로 셀 1개를 차지하도록 설정합니다.
        gbc.gridheight = 1;
        // 가로로 30%를 차지합니다.
        gbc.weightx = 0.3;
        // 세로로 반을 차지합니다.
        gbc.weighty = 0.8;
        panel.add(new JScrollPane(frequencyArea), gbc);

        // 오른쪽 하단 영역에는 압축/복원 전후의 용량을 표시합니다.
        // 셀 (1, 1)
        gbc.gridx = 1;
        gbc.gridy = 1;
        // 세로로 셀 1개를 차지하도록 설정합니다.
        gbc.gridheight = 1;
        // 가로로 30%를 차지합니다.
        gbc.weightx = 0.3;
        // 세로로 반을 차지합니다.
        gbc.weighty = 0.2;
        panel.add(new JScrollPane(volumeArea), gbc);

        // 최하단 영역에는 불러오기 및 압축/복원 및 저장을 수행하는 버튼을 표시합니다.
        // 셀 (0, 2)
        gbc.gridx = 0;
        gbc.gridy = 2;
        // 가로로 셀 2개를 차지하도록 설정합니다.
        gbc.gridwidth = 2;
        // 가로로 전체를 차지합니다.
        gbc.weightx = 1.0;
        // 세로로 5%를 차지하도록 설정하여 나머지 영역이 95%를 사용하도록 합니다.
        gbc.weighty = 0.05;
        panel.add(button, gbc);

        // 최하단 영역에는 불러오기 및 압축/복원 및 저장을 수행하는 버튼을 표시합니다.
        // 셀 (0, 2)
        gbc.gridx = 0;
        gbc.gridy = 3;
        // 가로로 셀 2개를 차지하도록 설정합니다.
        gbc.gridwidth = 2;
        // 가로로 전체를 차지합니다.
        gbc.weightx = 1.0;
        // 세로로 5%를 차지하도록 설정하여 나머지 영역이 95%를 사용하도록 합니다.
        gbc.weighty = 0.05;
        panel.add(button, gbc);

        // 프레임에 패널을 추가합니다.
        add(panel);

        // 프레임 사이즈 지정 후 화면 가운데 뜨도록 설정합니다.
        setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    @Override
    public void dragEnter(DropTargetDragEvent dtde) { }

    @Override
    public void dragOver(DropTargetDragEvent dtde) { }

    @Override
    public void dropActionChanged(DropTargetDragEvent dtde) { }

    @Override
    public void dragExit(DropTargetEvent dte) { }

    @Override
    public void drop(DropTargetDropEvent dtde) {
        try {
            dtde.acceptDrop(DnDConstants.ACTION_MOVE);
            List<File> files = (List<File>) dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
            file = files.get(0);
            run();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private class ButtonActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            int fileChooserReturnValue;

            // 선택 가능한 파일 확장자 초기화
            fileChooser.resetChoosableFileFilters();
            // 다중 선택이 불가하도록 설정
            fileChooser.setMultiSelectionEnabled(false);

            // 파일 탐색창 표시
            fileChooserReturnValue = fileChooser.showOpenDialog(null);
            // 파일 열기 버튼을 누르지 않았다면 이벤트 처리하지 않기
            if (fileChooserReturnValue != JFileChooser.APPROVE_OPTION) return;

            // 선택한 파일 경로 얻기
            file = fileChooser.getSelectedFile();

            if (file == null) return;

            run();
        }
    }

    void run() {
        setTitle("[진행 중...] " + APPLICATION_TITLE);

        // 화면 정리
        revalidate();
        repaint();

        String[] dilFileName = file.getName().split("\\.");
        try {
            if (!dilFileName[dilFileName.length - 1].equals("hfm")) {
                Compressor compressor = new Compressor(file.getPath());
                compressor.run();

                resultArea.setText(CompressorData.getResult(compressor));
                resultArea.select(0, 0);
                frequencyArea.setText(String.format("읽은 문자 개수: %d\n\n", CompressorData.getTotalChars(compressor)));
                frequencyArea.append(String.format("문자 개수: %d\n\n", CompressorData.getUsedChars(compressor)));
                frequencyArea.append(CompressorData.getFrequency(compressor));
                frequencyArea.select(0, 0);
                volumeArea.setText(String.format("압축 전 용량: %dBytes\n", CompressorData.getSrcVolume(compressor)));
                volumeArea.append(String.format("압축 후 용량: %dBytes\n", CompressorData.getOutVolume(compressor)));
                volumeArea.append(String.format("압축률: %f%s\n", (1-((double) CompressorData.getOutVolume(compressor) / CompressorData.getSrcVolume(compressor))) * 100, "%"));
                volumeArea.append(String.format("압축에 걸린 시간: %fms\n", CompressorData.getElapsedTime(compressor)));
                volumeArea.select(0, 0);

                compressor.close();
            } else {
                Decompressor decompressor = new Decompressor(file.getPath());
                decompressor.run();

                resultArea.setText(DecompressorData.getResult(decompressor));
                resultArea.select(0, 0);
                frequencyArea.setText(String.format("읽은 문자 개수: %d\n\n", DecompressorData.getTotalChars(decompressor)));
                volumeArea.setText(String.format("복원 전 용량: %dBytes\n", DecompressorData.getSrcVolume(decompressor)));
                volumeArea.append(String.format("복원 후 용량: %dBytes\n", DecompressorData.getOutVolume(decompressor)));
                volumeArea.append(String.format("복원에 걸린 시간: %fms\n", DecompressorData.getElapsedTime(decompressor)));
                volumeArea.select(0, 0);

                decompressor.close();
            }

            // 화면 정리
            revalidate();
            repaint();
        } catch (FileNotFoundException ex) {
            JOptionPane.showMessageDialog(getParent(), ex.getMessage(), "파일을 불러올 수 없습니다.", JOptionPane.ERROR_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(getParent(), ex.getMessage(), ex.getMessage(), JOptionPane.ERROR_MESSAGE);
        }

        setTitle(APPLICATION_TITLE);
    }
}
