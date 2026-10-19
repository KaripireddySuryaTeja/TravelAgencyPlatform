/**
 * seed-admin-data.js
 * Run with: mongosh mongodb://localhost:27017/travelagency seed-admin-data.js
 *
 * Seeds:
 *  1. Reviews   - mapped to actual current tour ObjectIds
 *  2. Bookings  - rich set of confirmed bookings for all 12 tours across 2025-2026
 *  3. Agent phone/messenger update from seed-data/agents.json
 */

// ── Current Tour ID Map (seed name → real ObjectId string) ──────────────────
const TOUR_IDS = {
  tour001: "69fc85e544b5079bbeabc117", // Paris City Escape
  tour002: "69fc85e544b5079bbeabc118", // Bali Tropical Retreat
  tour003: "69fc85e544b5079bbeabc119", // Rome & Vatican Tour
  tour004: "69fc85e544b5079bbeabc11a", // Dubai Luxury Experience
  tour005: "69fc85e544b5079bbeabc11b", // Tokyo & Kyoto Discovery
  tour006: "69fc85e544b5079bbeabc11c", // Santorini Sunset Cruise
  tour007: "69fc85e544b5079bbeabc11d", // New York City Adventure
  tour008: "69fc85e544b5079bbeabc11e", // Maldives Island Paradise
  tour009: "69fc85e544b5079bbeabc11f", // Safari in Kenya
  tour010: "69fc85e544b5079bbeabc120", // Barcelona & Costa Brava
  tour011: "69fc85e544b5079bbeabc121", // Swiss Alps Adventure
  tour012: "69fc85e544b5079bbeabc122", // Rajasthan Royal Heritage Tour
};

// ── Current Agent IDs ─────────────────────────────────────────────────────────
const AGENT_IDS = {
  agent001: "69fc85e544b5079bbeabc114", // Tyrone Boyer
  agent002: "69fc85e544b5079bbeabc115", // Sarah Mitchell
  agent003: "69fc85e544b5079bbeabc116", // Marco Rossi
};

// ─────────────────────────────────────────────────────────────────────────────
// 1. REVIEWS (from seed-data/reviews.json, tourId mapped to real IDs)
// ─────────────────────────────────────────────────────────────────────────────
const reviews = [
  { _id: "rev001", tourId: TOUR_IDS.tour001, userId: "user001", userName: "Emily Carter",      userAvatarUrl: "https://i.pravatar.cc/150?img=1",  rate: 5.0, comment: "Absolutely magical! The Eiffel Tower at night took my breath away. Hotel Le Marais was perfectly located.",            reviewDate: new Date("2026-04-10"), hidden: false, createdAt: new Date("2026-04-10") },
  { _id: "rev002", tourId: TOUR_IDS.tour001, userId: "user002", userName: "James Liu",         userAvatarUrl: "https://i.pravatar.cc/150?img=2",  rate: 4.5, comment: "Great trip overall. The guided Louvre tour was a highlight. Would have loved more free time.",                       reviewDate: new Date("2026-04-15"), hidden: false, createdAt: new Date("2026-04-15") },
  { _id: "rev003", tourId: TOUR_IDS.tour001, userId: "user003", userName: "Sophie Dupont",     userAvatarUrl: "https://i.pravatar.cc/150?img=3",  rate: 4.8, comment: "Wonderful experience! Food was amazing and the hotel staff were incredibly helpful.",                               reviewDate: new Date("2026-04-20"), hidden: false, createdAt: new Date("2026-04-20") },
  { _id: "rev004", tourId: TOUR_IDS.tour002, userId: "user004", userName: "Aisha Patel",       userAvatarUrl: "https://i.pravatar.cc/150?img=4",  rate: 5.0, comment: "Bali exceeded every expectation. The infinity pool villa was pure paradise. Will definitely return!",                reviewDate: new Date("2026-03-22"), hidden: false, createdAt: new Date("2026-03-22") },
  { _id: "rev005", tourId: TOUR_IDS.tour002, userId: "user005", userName: "Michael Torres",    userAvatarUrl: "https://i.pravatar.cc/150?img=5",  rate: 4.7, comment: "The rice terraces in Ubud were stunning. All inclusive package was excellent value.",                               reviewDate: new Date("2026-03-28"), hidden: false, createdAt: new Date("2026-03-28") },
  { _id: "rev006", tourId: TOUR_IDS.tour002, userId: "user006", userName: "Yuki Tanaka",       userAvatarUrl: "https://i.pravatar.cc/150?img=6",  rate: 4.9, comment: "A truly transformative trip. The temple visits and spa treatments were incredible.",                                 reviewDate: new Date("2026-04-01"), hidden: false, createdAt: new Date("2026-04-01") },
  { _id: "rev007", tourId: TOUR_IDS.tour003, userId: "user007", userName: "Marco Bianchi",     userAvatarUrl: "https://i.pravatar.cc/150?img=7",  rate: 4.8, comment: "History came alive in Rome! The Vatican Museum skip-the-line access was worth every penny.",                        reviewDate: new Date("2026-03-10"), hidden: false, createdAt: new Date("2026-03-10") },
  { _id: "rev008", tourId: TOUR_IDS.tour003, userId: "user008", userName: "Laura Hernandez",   userAvatarUrl: "https://i.pravatar.cc/150?img=8",  rate: 4.6, comment: "Fantastic tour with knowledgeable guides. The Colosseum visit was unforgettable.",                                  reviewDate: new Date("2026-03-15"), hidden: false, createdAt: new Date("2026-03-15") },
  { _id: "rev009", tourId: TOUR_IDS.tour003, userId: "user009", userName: "David Kim",         userAvatarUrl: "https://i.pravatar.cc/150?img=9",  rate: 4.5, comment: "Great historical experience. Wish there was more time at the Sistine Chapel.",                                      reviewDate: new Date("2026-03-20"), hidden: false, createdAt: new Date("2026-03-20") },
  { _id: "rev010", tourId: TOUR_IDS.tour004, userId: "user010", userName: "Priya Sharma",      userAvatarUrl: "https://i.pravatar.cc/150?img=10", rate: 4.7, comment: "Dubai is spectacular! The desert safari was the highlight. Burj Khalifa views were breathtaking.",                  reviewDate: new Date("2026-02-14"), hidden: false, createdAt: new Date("2026-02-14") },
  { _id: "rev011", tourId: TOUR_IDS.tour004, userId: "user011", userName: "Omar Al-Farsi",     userAvatarUrl: "https://i.pravatar.cc/150?img=11", rate: 4.4, comment: "Amazing luxury experience. The hotel was top-notch. Mall of Emirates exceeded expectations.",                       reviewDate: new Date("2026-02-20"), hidden: false, createdAt: new Date("2026-02-20") },
  { _id: "rev012", tourId: TOUR_IDS.tour005, userId: "user012", userName: "Hana Nakamura",     userAvatarUrl: "https://i.pravatar.cc/150?img=12", rate: 5.0, comment: "Japan is a dream destination and this tour delivered perfectly. Cherry blossoms in Kyoto were magical.",             reviewDate: new Date("2026-04-05"), hidden: false, createdAt: new Date("2026-04-05") },
  { _id: "rev013", tourId: TOUR_IDS.tour005, userId: "user013", userName: "Chris Walker",      userAvatarUrl: "https://i.pravatar.cc/150?img=13", rate: 4.9, comment: "Best trip of my life! The contrast between Tokyo and Kyoto was incredible. Food was outstanding.",                   reviewDate: new Date("2026-04-08"), hidden: false, createdAt: new Date("2026-04-08") },
  { _id: "rev014", tourId: TOUR_IDS.tour005, userId: "user014", userName: "Mei Chen",          userAvatarUrl: "https://i.pravatar.cc/150?img=14", rate: 4.8, comment: "The geisha district in Kyoto was unlike anything I have ever seen. Highly recommended!",                            reviewDate: new Date("2026-04-12"), hidden: false, createdAt: new Date("2026-04-12") },
  { _id: "rev015", tourId: TOUR_IDS.tour006, userId: "user015", userName: "Isabella Rossi",    userAvatarUrl: "https://i.pravatar.cc/150?img=15", rate: 4.9, comment: "The most romantic destination I have ever visited. Sunset from Oia was simply breathtaking.",                       reviewDate: new Date("2026-03-05"), hidden: false, createdAt: new Date("2026-03-05") },
  { _id: "rev016", tourId: TOUR_IDS.tour006, userId: "user016", userName: "Tom Bradley",       userAvatarUrl: "https://i.pravatar.cc/150?img=16", rate: 4.7, comment: "The caldera views from our room were incredible. Excellent food and wine throughout.",                               reviewDate: new Date("2026-03-10"), hidden: false, createdAt: new Date("2026-03-10") },
  { _id: "rev017", tourId: TOUR_IDS.tour007, userId: "user017", userName: "Rachel Green",      userAvatarUrl: "https://i.pravatar.cc/150?img=17", rate: 4.5, comment: "NYC is electric! Times Square at night, Central Park at sunrise — both unforgettable.",                            reviewDate: new Date("2026-04-18"), hidden: false, createdAt: new Date("2026-04-18") },
  { _id: "rev018", tourId: TOUR_IDS.tour007, userId: "user018", userName: "Carlos Mendez",     userAvatarUrl: "https://i.pravatar.cc/150?img=18", rate: 4.3, comment: "Great city tour. The Broadway show included in the package was a wonderful bonus.",                                 reviewDate: new Date("2026-04-22"), hidden: false, createdAt: new Date("2026-04-22") },
  { _id: "rev019", tourId: TOUR_IDS.tour008, userId: "user019", userName: "Nina Johansson",    userAvatarUrl: "https://i.pravatar.cc/150?img=19", rate: 5.0, comment: "Heaven on Earth. The overwater bungalow with glass floor was the most unique experience of my life.",              reviewDate: new Date("2026-03-30"), hidden: false, createdAt: new Date("2026-03-30") },
  { _id: "rev020", tourId: TOUR_IDS.tour008, userId: "user020", userName: "Ahmed Hassan",      userAvatarUrl: "https://i.pravatar.cc/150?img=20", rate: 5.0, comment: "Pristine waters, incredible marine life, and world-class service. Absolutely worth every penny.",                   reviewDate: new Date("2026-04-02"), hidden: false, createdAt: new Date("2026-04-02") },
  { _id: "rev021", tourId: TOUR_IDS.tour009, userId: "user021", userName: "Grace Okonkwo",     userAvatarUrl: "https://i.pravatar.cc/150?img=21", rate: 5.0, comment: "Witnessing the Great Migration was a life-changing experience. Our guide was exceptional.",                         reviewDate: new Date("2026-02-28"), hidden: false, createdAt: new Date("2026-02-28") },
  { _id: "rev022", tourId: TOUR_IDS.tour009, userId: "user022", userName: "Peter van der Berg", userAvatarUrl: "https://i.pravatar.cc/150?img=22", rate: 4.8, comment: "Spotted all Big Five on day two! The tented camp was surprisingly luxurious.",                                      reviewDate: new Date("2026-03-04"), hidden: false, createdAt: new Date("2026-03-04") },
  { _id: "rev023", tourId: TOUR_IDS.tour010, userId: "user023", userName: "Elena Fernandez",   userAvatarUrl: "https://i.pravatar.cc/150?img=23", rate: 4.8, comment: "Gaudi's architecture is truly one-of-a-kind. The tapas tour through the Gothic Quarter was delicious!",             reviewDate: new Date("2026-04-14"), hidden: false, createdAt: new Date("2026-04-14") },
  { _id: "rev024", tourId: TOUR_IDS.tour010, userId: "user024", userName: "Alex Murphy",       userAvatarUrl: "https://i.pravatar.cc/150?img=24", rate: 4.6, comment: "Barcelona has incredible energy. Sagrada Familia left me speechless. Great beach time too.",                        reviewDate: new Date("2026-04-19"), hidden: false, createdAt: new Date("2026-04-19") },
  { _id: "rev025", tourId: TOUR_IDS.tour011, userId: "user025", userName: "Lena Muller",       userAvatarUrl: "https://i.pravatar.cc/150?img=25", rate: 4.9, comment: "The Matterhorn views from Zermatt were jaw-dropping. Ski slopes were perfectly groomed.",                          reviewDate: new Date("2026-01-20"), hidden: false, createdAt: new Date("2026-01-20") },
  { _id: "rev026", tourId: TOUR_IDS.tour011, userId: "user026", userName: "John Stevens",      userAvatarUrl: "https://i.pravatar.cc/150?img=26", rate: 4.7, comment: "Switzerland in winter is a fairytale. Lake Geneva cruise and Lucerne were absolute highlights.",                    reviewDate: new Date("2026-01-25"), hidden: false, createdAt: new Date("2026-01-25") },
  { _id: "rev027", tourId: TOUR_IDS.tour012, userId: "user027", userName: "Ananya Gupta",      userAvatarUrl: "https://i.pravatar.cc/150?img=27", rate: 4.8, comment: "The palace hotels were spectacular. Camel ride in Thar Desert at sunset was unforgettable.",                       reviewDate: new Date("2026-02-10"), hidden: false, createdAt: new Date("2026-02-10") },
  { _id: "rev028", tourId: TOUR_IDS.tour012, userId: "user028", userName: "Robert Clark",      userAvatarUrl: "https://i.pravatar.cc/150?img=28", rate: 4.6, comment: "Rich culture, stunning architecture, and warm hospitality. Udaipur was the crown jewel.",                          reviewDate: new Date("2026-02-15"), hidden: false, createdAt: new Date("2026-02-15") },
  // A couple hidden reviews to test moderation
  { _id: "rev029", tourId: TOUR_IDS.tour001, userId: "user029", userName: "Spam User",         userAvatarUrl: "https://i.pravatar.cc/150?img=29", rate: 1.0, comment: "Buy cheap tours at spamsite.com !!!",                                                                               reviewDate: new Date("2026-05-01"), hidden: true,  createdAt: new Date("2026-05-01") },
  { _id: "rev030", tourId: TOUR_IDS.tour003, userId: "user030", userName: "Bot Account",       userAvatarUrl: "https://i.pravatar.cc/150?img=30", rate: 1.0, comment: "WORST TOUR EVER click here for refund form bit.ly/xxx",                                                            reviewDate: new Date("2026-05-10"), hidden: true,  createdAt: new Date("2026-05-10") },
];

// ─────────────────────────────────────────────────────────────────────────────
// 2. BOOKINGS — spread across Jan–May 2026, linked to current tour IDs
//    Agents: tour001,004,007,010 → agent001(Tyrone)
//            tour002,005,008,011 → agent002(Sarah)
//            tour003,006,009,012 → agent003(Marco)
// ─────────────────────────────────────────────────────────────────────────────
const CONFIRMED = "CONFIRMED";
const BOOKED    = "BOOKED"; // PENDING is not a valid BookingState; use BOOKED for upcoming bookings

const bookings = [
  // ─── Tyrone's tours (Paris, Dubai, NYC, Barcelona) ───────────────────────
  { tourId: TOUR_IDS.tour001, userId: "user001", date: new Date("2026-01-10"), adults: 2, children: 0, duration: "5 days", mealPlan: "BB", totalPrice: 1998,  state: CONFIRMED, createdAt: new Date("2025-12-15") },
  { tourId: TOUR_IDS.tour001, userId: "user002", date: new Date("2026-01-20"), adults: 1, children: 1, duration: "7 days", mealPlan: "HB", totalPrice: 1544,  state: CONFIRMED, createdAt: new Date("2025-12-20") },
  { tourId: TOUR_IDS.tour001, userId: "user003", date: new Date("2026-02-05"), adults: 2, children: 0, duration: "5 days", mealPlan: "BB", totalPrice: 1998,  state: CONFIRMED, createdAt: new Date("2026-01-10") },
  { tourId: TOUR_IDS.tour001, userId: "user004", date: new Date("2026-03-12"), adults: 2, children: 1, duration: "7 days", mealPlan: "HB", totalPrice: 2193,  state: CONFIRMED, createdAt: new Date("2026-02-15") },
  { tourId: TOUR_IDS.tour001, userId: "user005", date: new Date("2026-04-08"), adults: 1, children: 0, duration: "5 days", mealPlan: "BB", totalPrice: 999,   state: CONFIRMED, createdAt: new Date("2026-03-10") },
  { tourId: TOUR_IDS.tour001, userId: "user006", date: new Date("2026-05-03"), adults: 3, children: 0, duration: "7 days", mealPlan: "BB", totalPrice: 3897,  state: CONFIRMED, createdAt: new Date("2026-04-01") },

  { tourId: TOUR_IDS.tour004, userId: "user007", date: new Date("2026-01-15"), adults: 2, children: 0, duration: "7 days", mealPlan: "BB", totalPrice: 3598,  state: CONFIRMED, createdAt: new Date("2025-12-20") },
  { tourId: TOUR_IDS.tour004, userId: "user008", date: new Date("2026-02-18"), adults: 2, children: 2, duration: "7 days", mealPlan: "AI", totalPrice: 5196,  state: CONFIRMED, createdAt: new Date("2026-01-20") },
  { tourId: TOUR_IDS.tour004, userId: "user009", date: new Date("2026-03-22"), adults: 1, children: 0, duration: "5 days", mealPlan: "BB", totalPrice: 1799,  state: CONFIRMED, createdAt: new Date("2026-02-25") },
  { tourId: TOUR_IDS.tour004, userId: "user010", date: new Date("2026-04-14"), adults: 2, children: 0, duration: "7 days", mealPlan: "BB", totalPrice: 3598,  state: CONFIRMED, createdAt: new Date("2026-03-18") },

  { tourId: TOUR_IDS.tour007, userId: "user011", date: new Date("2026-01-25"), adults: 2, children: 0, duration: "5 days", mealPlan: "BB", totalPrice: 2398,  state: CONFIRMED, createdAt: new Date("2025-12-28") },
  { tourId: TOUR_IDS.tour007, userId: "user012", date: new Date("2026-03-05"), adults: 1, children: 0, duration: "7 days", mealPlan: "HB", totalPrice: 1649,  state: CONFIRMED, createdAt: new Date("2026-02-05") },
  { tourId: TOUR_IDS.tour007, userId: "user013", date: new Date("2026-04-20"), adults: 3, children: 1, duration: "5 days", mealPlan: "BB", totalPrice: 4796,  state: CONFIRMED, createdAt: new Date("2026-03-22") },

  { tourId: TOUR_IDS.tour010, userId: "user014", date: new Date("2026-02-10"), adults: 2, children: 0, duration: "7 days", mealPlan: "BB", totalPrice: 2998,  state: CONFIRMED, createdAt: new Date("2026-01-14") },
  { tourId: TOUR_IDS.tour010, userId: "user015", date: new Date("2026-03-28"), adults: 1, children: 0, duration: "5 days", mealPlan: "HB", totalPrice: 1244,  state: CONFIRMED, createdAt: new Date("2026-02-28") },
  { tourId: TOUR_IDS.tour010, userId: "user016", date: new Date("2026-05-05"), adults: 2, children: 1, duration: "7 days", mealPlan: "BB", totalPrice: 3747,  state: CONFIRMED, createdAt: new Date("2026-04-06") },

  // ─── Sarah's tours (Bali, Tokyo, Maldives, Swiss Alps) ───────────────────
  { tourId: TOUR_IDS.tour002, userId: "user017", date: new Date("2026-01-08"), adults: 2, children: 0, duration: "7 days", mealPlan: "BB", totalPrice: 2398,  state: CONFIRMED, createdAt: new Date("2025-12-10") },
  { tourId: TOUR_IDS.tour002, userId: "user018", date: new Date("2026-02-14"), adults: 2, children: 1, duration: "10 days", mealPlan: "AI", totalPrice: 5397, state: CONFIRMED, createdAt: new Date("2026-01-16") },
  { tourId: TOUR_IDS.tour002, userId: "user019", date: new Date("2026-03-18"), adults: 1, children: 0, duration: "7 days", mealPlan: "BB", totalPrice: 1199,  state: CONFIRMED, createdAt: new Date("2026-02-20") },
  { tourId: TOUR_IDS.tour002, userId: "user020", date: new Date("2026-04-22"), adults: 2, children: 0, duration: "10 days", mealPlan: "BB", totalPrice: 3198, state: CONFIRMED, createdAt: new Date("2026-03-25") },
  { tourId: TOUR_IDS.tour002, userId: "user021", date: new Date("2026-05-10"), adults: 2, children: 2, duration: "7 days", mealPlan: "AI", totalPrice: 5996,  state: CONFIRMED, createdAt: new Date("2026-04-12") },

  { tourId: TOUR_IDS.tour005, userId: "user022", date: new Date("2026-01-12"), adults: 2, children: 0, duration: "7 days", mealPlan: "BB", totalPrice: 3598,  state: CONFIRMED, createdAt: new Date("2025-12-15") },
  { tourId: TOUR_IDS.tour005, userId: "user023", date: new Date("2026-02-20"), adults: 1, children: 0, duration: "10 days", mealPlan: "HB", totalPrice: 2149, state: CONFIRMED, createdAt: new Date("2026-01-22") },
  { tourId: TOUR_IDS.tour005, userId: "user024", date: new Date("2026-03-30"), adults: 2, children: 1, duration: "7 days", mealPlan: "BB", totalPrice: 5397,  state: CONFIRMED, createdAt: new Date("2026-03-01") },
  { tourId: TOUR_IDS.tour005, userId: "user025", date: new Date("2026-05-08"), adults: 2, children: 0, duration: "14 days", mealPlan: "HB", totalPrice: 5598, state: CONFIRMED, createdAt: new Date("2026-04-10") },

  { tourId: TOUR_IDS.tour008, userId: "user026", date: new Date("2026-01-18"), adults: 2, children: 0, duration: "7 days", mealPlan: "AI", totalPrice: 6998,  state: CONFIRMED, createdAt: new Date("2025-12-22") },
  { tourId: TOUR_IDS.tour008, userId: "user027", date: new Date("2026-02-25"), adults: 2, children: 0, duration: "5 days", mealPlan: "BB", totalPrice: 4798,  state: CONFIRMED, createdAt: new Date("2026-01-28") },
  { tourId: TOUR_IDS.tour008, userId: "user028", date: new Date("2026-04-02"), adults: 2, children: 1, duration: "10 days", mealPlan: "AI", totalPrice: 9597, state: CONFIRMED, createdAt: new Date("2026-03-05") },

  { tourId: TOUR_IDS.tour011, userId: "user001", date: new Date("2026-01-05"), adults: 2, children: 0, duration: "7 days", mealPlan: "BB", totalPrice: 3998,  state: CONFIRMED, createdAt: new Date("2025-12-08") },
  { tourId: TOUR_IDS.tour011, userId: "user003", date: new Date("2026-02-15"), adults: 1, children: 0, duration: "5 days", mealPlan: "HB", totalPrice: 1949,  state: CONFIRMED, createdAt: new Date("2026-01-18") },
  { tourId: TOUR_IDS.tour011, userId: "user005", date: new Date("2026-04-10"), adults: 2, children: 2, duration: "7 days", mealPlan: "BB", totalPrice: 5996,  state: CONFIRMED, createdAt: new Date("2026-03-12") },

  // ─── Marco's tours (Rome, Santorini, Safari, Rajasthan) ──────────────────
  { tourId: TOUR_IDS.tour003, userId: "user002", date: new Date("2026-01-22"), adults: 2, children: 0, duration: "7 days", mealPlan: "BB", totalPrice: 2798,  state: CONFIRMED, createdAt: new Date("2025-12-25") },
  { tourId: TOUR_IDS.tour003, userId: "user004", date: new Date("2026-02-28"), adults: 2, children: 1, duration: "5 days", mealPlan: "HB", totalPrice: 2597,  state: CONFIRMED, createdAt: new Date("2026-01-30") },
  { tourId: TOUR_IDS.tour003, userId: "user006", date: new Date("2026-03-15"), adults: 1, children: 0, duration: "7 days", mealPlan: "BB", totalPrice: 1399,  state: CONFIRMED, createdAt: new Date("2026-02-17") },
  { tourId: TOUR_IDS.tour003, userId: "user008", date: new Date("2026-04-25"), adults: 2, children: 0, duration: "5 days", mealPlan: "BB", totalPrice: 2798,  state: CONFIRMED, createdAt: new Date("2026-03-28") },
  { tourId: TOUR_IDS.tour003, userId: "user010", date: new Date("2026-05-12"), adults: 3, children: 0, duration: "7 days", mealPlan: "HB", totalPrice: 5397,  state: CONFIRMED, createdAt: new Date("2026-04-15") },

  { tourId: TOUR_IDS.tour006, userId: "user011", date: new Date("2026-01-30"), adults: 2, children: 0, duration: "5 days", mealPlan: "BB", totalPrice: 2598,  state: CONFIRMED, createdAt: new Date("2026-01-02") },
  { tourId: TOUR_IDS.tour006, userId: "user013", date: new Date("2026-03-10"), adults: 2, children: 0, duration: "7 days", mealPlan: "HB", totalPrice: 3398,  state: CONFIRMED, createdAt: new Date("2026-02-12") },
  { tourId: TOUR_IDS.tour006, userId: "user015", date: new Date("2026-04-18"), adults: 1, children: 0, duration: "5 days", mealPlan: "BB", totalPrice: 1299,  state: CONFIRMED, createdAt: new Date("2026-03-20") },

  { tourId: TOUR_IDS.tour009, userId: "user017", date: new Date("2026-02-05"), adults: 2, children: 0, duration: "7 days", mealPlan: "FB", totalPrice: 4598,  state: CONFIRMED, createdAt: new Date("2026-01-08") },
  { tourId: TOUR_IDS.tour009, userId: "user019", date: new Date("2026-03-20"), adults: 2, children: 1, duration: "10 days", mealPlan: "FB", totalPrice: 6897, state: CONFIRMED, createdAt: new Date("2026-02-22") },
  { tourId: TOUR_IDS.tour009, userId: "user021", date: new Date("2026-05-01"), adults: 1, children: 0, duration: "7 days", mealPlan: "FB", totalPrice: 2299,  state: CONFIRMED, createdAt: new Date("2026-04-03") },

  { tourId: TOUR_IDS.tour012, userId: "user023", date: new Date("2026-01-28"), adults: 2, children: 0, duration: "7 days", mealPlan: "BB", totalPrice: 2998,  state: CONFIRMED, createdAt: new Date("2025-12-30") },
  { tourId: TOUR_IDS.tour012, userId: "user025", date: new Date("2026-02-22"), adults: 2, children: 2, duration: "10 days", mealPlan: "HB", totalPrice: 5996, state: CONFIRMED, createdAt: new Date("2026-01-25") },
  { tourId: TOUR_IDS.tour012, userId: "user027", date: new Date("2026-04-05"), adults: 1, children: 0, duration: "5 days", mealPlan: "BB", totalPrice: 1499,  state: CONFIRMED, createdAt: new Date("2026-03-07") },

  // A few upcoming BOOKED bookings for variety
  { tourId: TOUR_IDS.tour001, userId: "user028", date: new Date("2026-06-10"), adults: 2, children: 0, duration: "7 days", mealPlan: "HB", totalPrice: "2948", state: BOOKED, createdAt: new Date("2026-05-20") },
  { tourId: TOUR_IDS.tour005, userId: "user026", date: new Date("2026-06-20"), adults: 2, children: 1, duration: "10 days", mealPlan: "BB", totalPrice: "5397", state: BOOKED, createdAt: new Date("2026-05-25") },
];

// ─────────────────────────────────────────────────────────────────────────────
// 3. RUN SEEDING
// ─────────────────────────────────────────────────────────────────────────────
print("\n=== Seeding Reviews ===");
let reviewsInserted = 0, reviewsSkipped = 0;
reviews.forEach(r => {
  if (db.reviews.findOne({ _id: r._id })) {
    reviewsSkipped++;
  } else {
    db.reviews.insertOne(r);
    reviewsInserted++;
  }
});
print(`Reviews: ${reviewsInserted} inserted, ${reviewsSkipped} already existed`);

print("\n=== Seeding Bookings ===");
let bookingsInserted = 0;
bookings.forEach(b => {
  db.bookings.insertOne(b);
  bookingsInserted++;
});
print(`Bookings: ${bookingsInserted} inserted`);

print("\n=== Updating Agent Phone/Messenger Info ===");
db.users.updateOne(
  { _id: ObjectId(AGENT_IDS.agent001) },
  { $set: { phone: "480-221-1885", messengerLink: "Messenger", updatedAt: new Date() } }
);
db.users.updateOne(
  { _id: ObjectId(AGENT_IDS.agent002) },
  { $set: { phone: "312-555-0198", messengerLink: "WhatsApp", updatedAt: new Date() } }
);
db.users.updateOne(
  { _id: ObjectId(AGENT_IDS.agent003) },
  { $set: { phone: "39-06-555-0187", messengerLink: "Telegram", updatedAt: new Date() } }
);
print("Agent contact info updated");

print("\n=== Final Collection Counts ===");
db.getCollectionNames().forEach(c => print(`  ${c}: ${db[c].countDocuments()}`));
print("\n✅ Seeding complete!");

