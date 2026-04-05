package com.klu.controller;

import com.klu.entity.Booking;
import com.klu.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class BookingController {

    @Autowired
    private BookingService service;

    @PostMapping("/booking")
    public Booking createBooking(@RequestBody Booking booking) {
        return service.createBooking(booking);
    }

    @GetMapping("/bookings/{userId}")
    public List<Booking> getBookings(@PathVariable int userId) {
        return service.getBookings(userId);
    }
}