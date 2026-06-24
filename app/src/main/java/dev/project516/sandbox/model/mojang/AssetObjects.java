package dev.project516.sandbox.model.mojang;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AssetObjects (Map<String, AssetObject> objects){
}
