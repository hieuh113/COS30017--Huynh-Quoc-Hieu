# Music Studio Rental App - UX Documentation

## User Stories

### User Story 1: Browse Rental Items
**As a** music studio client  
**I want to** browse through available musical instruments and equipment  
**So that** I can see what items are available for rental and their details

**Acceptance Criteria:**
- I can see one item at a time with its image, name, rating, category, and price
- I can navigate between items using Previous/Next buttons
- I can see the item's star rating (0-5 stars)
- I can see the item's category (e.g., "String Instruments", "Keyboard Instruments")
- I can see the monthly rental price in credits

### User Story 2: Book a Rental Item
**As a** music studio client  
**I want to** book a rental item for a specific duration  
**So that** I can rent the equipment I need for my musical activities

**Acceptance Criteria:**
- I can click "Borrow This Item" to start the booking process
- I can enter my personal information (name and email)
- I can select rental duration (1, 3, or 6 months)
- I can see the total cost before confirming
- I receive confirmation when the booking is successful
- I can cancel the booking if needed

## Use Cases

### Use Case 1: Browse and Select Rental Item
**Primary Actor:** Music Studio Client  
**Goal:** Browse available rental items and select one for booking

**Preconditions:**
- User has opened the app
- Rental items are available in the system

**Main Flow:**
1. User opens the app and sees the first rental item
2. User views item details (image, name, rating, category, price)
3. User can navigate to other items using Previous/Next buttons
4. User finds an item they want to rent
5. User clicks "Borrow This Item" button
6. System navigates to booking detail screen

**Alternative Flows:**
- 3a. If user is on the first item, Previous button is disabled
- 3b. If user is on the last item, Next button is disabled

### Use Case 2: Complete Rental Booking
**Primary Actor:** Music Studio Client  
**Goal:** Complete the booking process for a selected rental item

**Preconditions:**
- User has selected a rental item from the main screen
- User is on the booking detail screen

**Main Flow:**
1. User sees the selected item details and booking form
2. User enters their full name
3. User enters their email address
4. User selects rental duration (1, 3, or 6 months)
5. System calculates and displays total cost
6. User clicks "Save Booking" button
7. System validates the input data
8. System creates the booking and shows confirmation
9. User is returned to main screen with booking status displayed

**Alternative Flows:**
- 7a. If validation fails, system shows error messages and prevents booking
- 7b. If user clicks "Cancel", booking is cancelled and user returns to main screen

## Layout Sketches

### Layout Option 1: Card-Based Design

```
┌─────────────────────────────────────┐
│        Music Studio Rentals         │
│                                     │
│    ┌─────────────────────────────┐   │
│    │                             │   │
│    │        [ITEM IMAGE]         │   │
│    │                             │   │
│    └─────────────────────────────┘   │
│                                     │
│           Electric Guitar           │
│           ★★★★☆ (4.5)              │
│           Category: String          │
│           Price: 50 credits/month  │
│                                     │
│    [Previous]    [Next]             │
│                                     │
│    ┌─────────────────────────────┐   │
│    │     Borrow This Item        │   │
│    └─────────────────────────────┘   │
│                                     │
│    Booked: Electric Guitar for 3    │
│    month(s)                         │
└─────────────────────────────────────┘
```

**Justification for Layout Option 1:**
- **Card-based design** provides clear visual separation of content
- **Large image display** allows users to clearly see the item
- **Rating stars** provide immediate visual feedback on item quality
- **Clear navigation buttons** make it easy to browse items
- **Prominent "Borrow" button** encourages action
- **Booking status display** shows current rental information

### Layout Option 2: List-Based Design

```
┌─────────────────────────────────────┐
│        Music Studio Rentals         │
│                                     │
│  ┌─────────────────────────────────┐ │
│  │ [IMG] Electric Guitar           │ │
│  │      ★★★★☆ String Instruments  │ │
│  │      50 credits/month           │ │
│  └─────────────────────────────────┘ │
│                                     │
│  ┌─────────────────────────────────┐ │
│  │ [IMG] Digital Piano             │ │
│  │      ★★★★★ Keyboard            │ │
│  │      80 credits/month           │ │
│  └─────────────────────────────────┘ │
│                                     │
│  ┌─────────────────────────────────┐ │
│  │ [IMG] Drum Kit                  │ │
│  │      ★★★★☆ Percussion           │ │
│  │      120 credits/month          │ │
│  └─────────────────────────────────┘ │
│                                     │
│  ┌─────────────────────────────────┐ │
│  │ [IMG] Microphone                │ │
│  │      ★★★★★ Audio Equipment     │ │
│  │      30 credits/month            │ │
│  └─────────────────────────────────┘ │
│                                     │
│    [Previous]    [Next]             │
│                                     │
│    ┌─────────────────────────────┐   │
│    │     Borrow This Item        │   │
│    └─────────────────────────────┘   │
└─────────────────────────────────────┘
```

**Justification for Layout Option 2:**
- **List format** allows users to see multiple items at once
- **Compact information display** shows all key details efficiently
- **Consistent layout** makes it easy to compare items
- **Smaller images** allow more items to be visible
- **Horizontal navigation** still provides item-by-item browsing
- **Space-efficient design** maximizes screen real estate

## Design Choice Rationale

**Selected Layout: Card-Based Design (Option 1)**

**Reasons for Selection:**
1. **Mobile-first approach**: The card design works better on mobile screens where space is limited
2. **Focus on single item**: The requirement specifies showing "one item at a time", which the card design supports better
3. **Visual hierarchy**: Large image and clear typography create better visual impact
4. **Touch-friendly**: Larger buttons and clear spacing improve usability on mobile devices
5. **Rating display**: The card design better accommodates the RatingBar widget requirement
6. **Booking status**: Clear area for displaying booking confirmation information

**UI Elements Used:**
- **RatingBar**: For displaying item ratings (0-5 stars)
- **RadioGroup**: For selecting rental duration (1, 3, or 6 months)
- **TextInputLayout**: For customer information input with validation
- **Button**: For navigation and actions
- **ImageView**: For displaying item images
- **TextView**: For displaying item information

**Accessibility Considerations:**
- Clear contrast between text and background
- Adequate touch target sizes for buttons
- Logical tab order for navigation
- Descriptive text for screen readers
- Error messages that are clearly visible

