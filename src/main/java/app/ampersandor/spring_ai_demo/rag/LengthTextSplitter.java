package app.ampersandor.spring_ai_demo.rag;

import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal text splitter used during the RAG ETL step.
 * Extends Spring AI's {@link TextSplitter} helper so it can be plugged into the document pipeline.
 */
public class LengthTextSplitter extends TextSplitter {
    private final int chunkSize;
    private final int chunkOverlap;

    public LengthTextSplitter(int chunkSize, int chunkOverlap) {
        if (chunkSize <= 0) throw new IllegalArgumentException("chunkSize must be positive.");
        if (chunkOverlap < 0 || chunkOverlap >= chunkSize)
            throw new IllegalArgumentException("chunkOverlap must be >= 0 and < chunkSize.");
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
    }

    protected List<String> splitText(String text) {
        List<String> chunks = new ArrayList<>();
        // 텍스트가 비어있거나 공백이면 빈 리스트 반환
        if (!StringUtils.hasText(text))
            return chunks;

        int textLength = text.length();
        // 텍스트 길이가 overlap보다 작거나 같으면 전체를 하나의 청크로 처리
        if (textLength <= chunkOverlap) {
            chunks.add(text);
            return chunks;
        }

        int position = 0;
        // 청크 사이즈 단위로 텍스트를 분할하되, overlap 만큼 겹치게 이동
        while (position < textLength) {
            int end = Math.min(position + chunkSize, textLength);
            chunks.add(text.substring(position, end));
            int nextPosition = end - chunkOverlap;
            // nextPosition이 더 이상 앞으로 나아가지 않으면 정지
            if (nextPosition <= position) {
                break;
            }
            position = nextPosition;
        }
        return chunks;
    }
}
