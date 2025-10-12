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

import java.util.List;

public record AnnouncementManifest(
        String latest_version,
        List<Announcement> messages
) {
    public static final Codec<AnnouncementManifest> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("latest_version").forGetter(AnnouncementManifest::latest_version),
            Announcement.CODEC.listOf().fieldOf("messages").forGetter(AnnouncementManifest::messages)
    ).apply(instance, AnnouncementManifest::new));

    public static AnnouncementManifest empty() {
        return new AnnouncementManifest("0.0.0", List.of());
    }
}