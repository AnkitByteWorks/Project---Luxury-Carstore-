package com.Luxurycars.carstore.entity;


public enum OrderStatus {
    PENDING,        // just placed, awaiting confirmation
    PROCESSING,     // undergoing vehicle preparation / verification
    CONFIRMED,      // showroom confirmed
    SHIPPED,        // on the way / en route
    DELIVERED,      // customer has the car
    CANCELLED       // cancelled by customer or admin
}


