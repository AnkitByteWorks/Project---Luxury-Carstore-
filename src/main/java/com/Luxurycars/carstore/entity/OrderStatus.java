package com.Luxurycars.carstore.entity;


public enum OrderStatus {
    PENDING,        // just placed, awaiting confirmation
    CONFIRMED,      // showroom confirmed
    SHIPPED,        // on the way
    DELIVERED,      // customer has the car
    CANCELLED       // cancelled by customer or admin
}


