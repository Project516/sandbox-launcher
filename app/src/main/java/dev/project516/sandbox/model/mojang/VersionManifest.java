package dev.project516.sandbox.model.mojang;

import com.fasterxml.jackson.core.Version;
import java.util.List;

public record VersionManifest(List<Version> versions) {}
