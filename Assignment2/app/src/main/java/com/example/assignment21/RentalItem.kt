package com.example.assignment21

import android.os.Parcel
import android.os.Parcelable

data class RentalItem(
    val name: String,
    val rating: Float,
    val category: String,
    val pricePerMonth: Int,
    val imageResource: Int,
    val isAvailable: Boolean = true
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readFloat(),
        parcel.readString() ?: "",
        parcel.readInt(),
        parcel.readInt(),
        parcel.readByte() != 0.toByte()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeFloat(rating)
        parcel.writeString(category)
        parcel.writeInt(pricePerMonth)
        parcel.writeInt(imageResource)
        parcel.writeByte(if (isAvailable) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<RentalItem> {
        override fun createFromParcel(parcel: Parcel): RentalItem {
            return RentalItem(parcel)
        }

        override fun newArray(size: Int): Array<RentalItem?> {
            return arrayOfNulls(size)
        }
    }
}

