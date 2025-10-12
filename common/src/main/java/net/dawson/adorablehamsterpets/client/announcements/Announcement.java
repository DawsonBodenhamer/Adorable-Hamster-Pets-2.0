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
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;

public record Announcement(
        String id,
        String kind,
        String semver,
        String title,
        String markdown,
        boolean mandatory,
        ZonedDateTime published
) {
    // Custom codec for ZonedDateTime using xmap to convert to/from String
    private static final Codec<ZonedDateTime> ZONED_DATE_TIME_CODEC = Codec.STRING.comapFlatMap(
            s -> {
                try {
                    return DataResult.success(ZonedDateTime.parse(s));
                } catch (DateTimeParseException e) {
                    return DataResult.error(() -> "Not a valid ZonedDateTime: " + e.getMessage());
                }
            },
            ZonedDateTime::toString
    );
    public static final Codec<Announcement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(Announcement::id),
            Codec.STRING.fieldOf("kind").forGetter(Announcement::kind),
            Codec.STRING.fieldOf("semver").forGetter(Announcement::semver),
            Codec.STRING.fieldOf("title").forGetter(Announcement::title),
            Codec.STRING.fieldOf("markdown").forGetter(Announcement::markdown),
            Codec.BOOL.fieldOf("mandatory").forGetter(Announcement::mandatory),
            ZONED_DATE_TIME_CODEC.fieldOf("published").forGetter(Announcement::published)
    ).apply(instance, Announcement::new));
}