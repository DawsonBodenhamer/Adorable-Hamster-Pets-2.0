package net.dawson.adorablehamsterpets.client.announcements;


/*
 * All Rights Reserved
 * Copyright (c) 2025 Dawson Bodenhamer (www.ForTheKing.Design)
 * 
 * All files and assets in this repository are the exclusive property of the copyright holder.
 * Permission is NOT granted to copy, modify, merge, publish, distribute, sublicense, or sell this material.
 * Provided "AS IS" without warranty. See LICENSE for details.
 */

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.dynamic.Codecs;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public record ClientAnnouncementState(
        Set<String> seen_ids,
        Map<String, Instant> snoozed_ids,
        String last_acknowledged_update,
        boolean opt_out_announcements,
        Optional<String> manifest_etag,
        Optional<String> manifest_last_modified
) {
    // Custom codec for a Set of Strings, built from a List codec
    private static final Codec<Set<String>> STRING_SET_CODEC = Codec.STRING.listOf().xmap(Set::copyOf, List::copyOf);
    private static final Codec<Map<String, Instant>> SNOOZE_MAP_CODEC = Codec.unboundedMap(Codec.STRING, Codecs.INSTANT);

    public static final Codec<ClientAnnouncementState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            STRING_SET_CODEC.fieldOf("seen_ids").forGetter(ClientAnnouncementState::seen_ids),
            SNOOZE_MAP_CODEC.optionalFieldOf("snoozed_ids", Map.of()).forGetter(ClientAnnouncementState::snoozed_ids),
            Codec.STRING.fieldOf("last_acknowledged_update").forGetter(ClientAnnouncementState::last_acknowledged_update),
            Codec.BOOL.fieldOf("opt_out_announcements").forGetter(ClientAnnouncementState::opt_out_announcements),
            Codec.STRING.optionalFieldOf("manifest_etag").forGetter(ClientAnnouncementState::manifest_etag),
            Codec.STRING.optionalFieldOf("manifest_last_modified").forGetter(ClientAnnouncementState::manifest_last_modified)
    ).apply(instance, ClientAnnouncementState::new));

    public static ClientAnnouncementState createDefault() {
        return new ClientAnnouncementState(
                Set.of(),
                Map.of(),
                "0.0.0",
                false,
                Optional.empty(),
                Optional.empty()
        );
    }
}