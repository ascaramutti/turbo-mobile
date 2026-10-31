package com.turbo.mobile.data.db.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "payments",
        foreignKeys = @ForeignKey(entity = BookingEntity.class, parentColumns = "id", childColumns = "booking_id",
                onDelete = ForeignKey.RESTRICT),
        indices = {@Index(value = "booking_id", unique = true), @Index("paid_at")})
public class PaymentEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "booking_id")
    public long bookingId;

    @ColumnInfo(name = "amount_cents")
    public int amountCents;

    @ColumnInfo(name = "platform_fee_cents")
    public int platformFeeCents;

    @NonNull
    @ColumnInfo(name = "card_last4")
    public String cardLast4 = "";

    @ColumnInfo(name = "paid_at")
    public long paidAt;
}
