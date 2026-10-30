package io.karatelabs.ext.contract;

import java.util.List;
import java.util.Map;

public record Operation(String id, Address address, Interaction interaction, List<Message> inputs, List<Message> outputs, List<Outcome> outcomes, String summary, List<String> tags, boolean deprecated, Map<String, Object> ext) {

}
