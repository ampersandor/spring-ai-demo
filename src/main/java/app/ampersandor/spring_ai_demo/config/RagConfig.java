package app.ampersandor.spring_ai_demo.config;

import app.ampersandor.spring_ai_demo.rag.LengthTextSplitter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.document.DocumentTransformer;
import org.springframework.ai.document.DocumentWriter;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.model.transformer.KeywordMetadataEnricher;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.postretrieval.document.DocumentPostProcessor;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.TranslationQueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

@Configuration
@ConditionalOnProperty(name = "app.mode", havingValue = "rag")
public class RagConfig {

    /**
     * Loads arbitrary PDF (or other Tika-supported) documents matching the
     * configured glob expression.
     * Each {@link DocumentReader} produces a collection of {@link Document} objects
     * that later flow through the ETL pipeline.
     */
    @Bean
    public DocumentReader[] documentReaders(
            @Value("${app.rag.documents-location-pattern}") String documentsLocationPattern) throws IOException {
        return Arrays.stream(new PathMatchingResourcePatternResolver().getResources(documentsLocationPattern))
                .map(TikaDocumentReader::new).toArray(DocumentReader[]::new);
    }

    /**
     * Splits long source documents into overlapping chunks to keep each RAG context
     * within the token budget.
     * Chunks are 400 characters with a 200 character overlap to preserve continuity
     * between slices.
     */
    @Bean
    public DocumentTransformer textSplitter() {
        // return new TokenTextSplitter();
        return new LengthTextSplitter(400, 200);
    }

    /**
     * Asks the underlying LLM to extract keywords, enriching every chunk with
     * metadata.
     * The metadata is stored alongside the vector embedding and can later be used
     * for filtering or display.
     * ChatModel을 이용해 각 문서 내용의 핵심 키워드를 추출
     * metadata 에 콤마(,)로 구분된 키워드 문자열이 excerpt_keywords 키추가
     * 문서 태깅, 검색 용이성 향상
     */
    @Bean
    public DocumentTransformer keywordMetadataEnricher(ChatModel chatModel) {
        // new SummaryMetadataEnricher();
        return new KeywordMetadataEnricher(chatModel, 3);
    }

    /**
     * Writes the transformed documents in JSON form to the console so you can see
     * what is being indexed.
     * DocumentWriter is a functional interface so you can implement your own
     * writers to push chunks to databases, search engines, etc.
     * The writer is used when the ETL pipeline calls
     * documentWriter.write(documents).
     * Spring AI autowires a Jackson {@link ObjectMapper}, so we reuse it for
     * pretty-printing.
     */
    @Bean
    public DocumentWriter jsonConsoleDocumentWriter(ObjectMapper objectMapper) {
        return documents -> {
            System.out.printf("======= save chunks size %d ========\n", documents.size());
            try {
                System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(documents));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            System.out.println("====================================");
        };
    }

    @ConditionalOnProperty(prefix = "app.vectorstore.in-memory", name = "enabled", havingValue = "true")
    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }

    /**
     * End-to-end ETL pipeline that executes at application start (when enabled).
     * 1. Extract: read the source files into {@link Document} objects.
     * 2. Transform: split and enrich each document.
     * 3. Load: push the transformed chunks to the configured
     * {@link DocumentWriter}s (e.g. vector store).
     */
    @ConditionalOnProperty(prefix = "app.etl.pipeline", name = "init", havingValue = "true")
    @Order(1) // cli 보다 먼저 실행
    @Bean
    public ApplicationRunner initEtlPipeline(DocumentReader[] documentReaders, DocumentTransformer textSplitter,
            DocumentTransformer keywordMetadataEnricher,
            DocumentWriter[] documentWriters) {
        return args ->
        // Extract
        Arrays.stream(documentReaders).map(DocumentReader::read)
                // Transform
                .map(textSplitter)
                .map(keywordMetadataEnricher)
                // Load
                .forEach(documents -> Arrays.stream(documentWriters)
                        .forEach(documentWriter -> documentWriter.write(documents)));
    }

    /**
     * Advisor that wires Retrieval-Augmented Generation into every chat call.
     * The {@link RetrievalAugmentationAdvisor} orchestrates query rewriting, vector
     * search and document post-processing.
     * This bean focuses on the retrieval piece and exposes an optional post
     * processor hook for the CLI.
     */
    @Bean
    public RetrievalAugmentationAdvisor retrievalAugmentationAdvisor(VectorStore vectorStore,
             ChatClient.Builder chatClientBuilder,
            Optional<DocumentPostProcessor> documentsPostProcessor) {
        RetrievalAugmentationAdvisor.Builder retrievalAugmentationAdvisorBuilder = RetrievalAugmentationAdvisor
                .builder()
                .queryExpander(MultiQueryExpander.builder().chatClientBuilder(chatClientBuilder).build())
                .queryTransformers(TranslationQueryTransformer.builder().chatClientBuilder(chatClientBuilder)
                        .targetLanguage("korean").build())
                .queryAugmenter(ContextualQueryAugmenter.builder().allowEmptyContext(true).build())
                .documentRetriever(VectorStoreDocumentRetriever.builder().similarityThreshold(0.3).topK(3)
                        .vectorStore(vectorStore).build());
        // .documentPostProcessors() // 뒤에서도 document post process 까지 이렇게 5개의 설정을 해볼 수 있다.;
        // RAG CLI 를 위해 등록
        documentsPostProcessor.ifPresent(retrievalAugmentationAdvisorBuilder::documentPostProcessors);
        return retrievalAugmentationAdvisorBuilder.build();
    }

    /**
     * Simple post processor that prints the retrieved documents and their scores so
     * you can inspect the context.
     * Returning the original list keeps the pipeline intact for downstream answer
     * generation.
     */
    @Bean
    public DocumentPostProcessor printDocumentsPostProcessor() {
        return (query, documents) -> {
            System.out.println("\n[ Search Results ]");
            System.out.println("===============================================");

            if (documents.isEmpty()) {
                System.out.println("  No search results found.");
                System.out.println("===============================================");
                return documents;
            }

            for (int i = 0; i < documents.size(); i++) {
                Document document = documents.get(i);
                System.out.printf("▶ %d Document, Score: %.2f%n", i + 1, document.getScore());
                System.out.println("-----------------------------------------------");
                Optional.ofNullable(document.getText()).stream()
                        .map(text -> text.split("\n")).flatMap(Arrays::stream)
                        .forEach(line -> System.out.printf("%s%n", line));
                System.out.println("===============================================");
            }
            System.out.print("\n[ RAG 사용 응답 ]\n\n");
            return documents;
        };
    }

}
