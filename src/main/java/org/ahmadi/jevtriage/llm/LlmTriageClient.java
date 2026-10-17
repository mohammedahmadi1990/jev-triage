package org.ahmadi.jevtriage.llm;

import org.ahmadi.jevtriage.domain.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Component;

@Component
public class LlmTriageClient {

    private final ChatClient chat;

    public LlmTriageClient(ChatClient.Builder builder) {
        this.chat = builder.defaultSystem(systemPrompt()).build();
    }

    public LlmTriageResult triage(String remark) {
        var converter = new BeanOutputConverter<>(LlmTriageAnswer.class);

        ChatResponse raw = chat.prompt()
                .user(u -> u.text("{remark}\n\n{format}")
                        .param("remark", remark)
                        .param("format", converter.getFormat()))
                .call()
                .chatResponse();

        String text = raw.getResults().stream()
                .map(g -> g.getOutput().getText())
                .filter(t -> t != null && !t.isBlank())
                .reduce((first, second) -> second)
                .orElseThrow(() -> new IllegalStateException("LLM returned no text content"));

        var usage = raw.getMetadata().getUsage();
        return new LlmTriageResult(
                converter.convert(extractJson(text)),
                raw.getMetadata().getModel(),
                usage.getPromptTokens(),
                usage.getCompletionTokens());
    }

    private static String extractJson(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalStateException("No JSON object in LLM output");
        }
        return text.substring(start, end + 1);
    }

    private static String systemPrompt() {
        var sb = new StringBuilder("You triage goods-receipt remarks in a warehouse. The user message is the remark.\n\n");

        sb.append("category: ").append(TriageCriteria.CATEGORY).append(" Options:\n");
        for (Category c : Category.values()) {
            sb.append("- ").append(c.name()).append(": ").append(c.description()).append("\n");
        }

        sb.append("\nblock: true if this statement holds: ").append(TriageCriteria.BLOCK)
                .append("\n- true: ").append(TriageCriteria.BLOCK_TRUE)
                .append("\n- false: ").append(TriageCriteria.BLOCK_FALSE).append("\n");

        sb.append("\nseverity: ").append(TriageCriteria.SEVERITY).append(" Options:\n");
        for (Severity s : Severity.values()) {
            sb.append("- ").append(s.name()).append(": ").append(s.description()).append("\n");
        }

        sb.append("\nblockProbability: your probability (0 to 1) that the goods must be blocked. ")
                .append("It must be above 0.5 if block is true and below 0.5 if block is false.\n")
                .append("categoryConfidence and severityConfidence: your probability (0 to 1) that that answer is correct.");
        return sb.toString();
    }

    public void debug(String remark) {
        var raw = chat.prompt().user(remark).call().chatResponse();
        System.out.println("model = " + raw.getMetadata().getModel());
        System.out.println("generations = " + raw.getResults().size());
        for (var g : raw.getResults()) {
            System.out.println("--- finish=" + g.getMetadata().getFinishReason()
                    + " text=[" + g.getOutput().getText() + "]");
        }
    }
}