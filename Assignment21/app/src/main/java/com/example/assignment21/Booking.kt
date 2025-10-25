package com.example.assignment21

import android.os.Parcel
import android.os.Parcelable

data class Booking(
    val rentalItem: RentalItem,
    val customerName: String,
    val customerEmail: String,
    val rentalDuration: Int, // in months
    val totalCost: Int,
    val isConfirmed: Boolean = false
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readParcelable(RentalItem::class.java.classLoader, RentalItem::class.java) ?: RentalItem("", 0f, "", 0, 0),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readInt(),
        parcel.readInt(),
        parcel.readByte() != 0.toByte()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(rentalItem, flags)
        parcel.writeString(customerName)
        parcel.writeString(customerEmail)
        parcel.writeInt(rentalDuration)
        parcel.writeInt(totalCost)
        parcel.writeByte(if (isConfirmed) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Booking> {
        override fun createFromParcel(parcel: Parcel): Booking {
            return Booking(parcel)
        }

        override fun newArray(size: Int): Array<Booking?> {
            return arrayOfNulls(size)
        }
    }
}

