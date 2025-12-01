package com.example.clearing.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Allows the allocation endpoint to accept either a single allocation object (legacy clients)
 * or a payload with an {@code allocations} array (new batch behavior).
 */
public class AllocationBatchRequestDeserializer extends StdDeserializer<AllocationBatchRequest> {

    protected AllocationBatchRequestDeserializer() {
        super(AllocationBatchRequest.class);
    }

    @Override
    public AllocationBatchRequest deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        ObjectCodec codec = p.getCodec();
        JsonNode root = codec.readTree(p);
        List<AllocationRequest> allocations = new ArrayList<>();
        boolean single = false;

        if (root.has("allocations")) {
            JsonNode allocationsNode = root.get("allocations");
            if (!allocationsNode.isArray()) {
                throw ctxt.mappingException("allocations must be an array");
            }
            ArrayNode arrayNode = (ArrayNode) allocationsNode;
            for (JsonNode entry : arrayNode) {
                allocations.add(convertNode(entry, codec));
            }
        } else if (root.isObject()) {
            allocations.add(convertNode(root, codec));
            single = true;
        } else {
            throw ctxt.mappingException("Invalid allocation payload");
        }

        if (allocations.isEmpty()) {
            throw ctxt.mappingException("allocations list cannot be empty");
        }

        return new AllocationBatchRequest(allocations, single);
    }

    private AllocationRequest convertNode(JsonNode node, ObjectCodec codec) throws IOException {
        ObjectMapper mapper = (ObjectMapper) codec;
        return mapper.treeToValue(node, AllocationRequest.class);
    }
}
