package com.epam.edp.demo;

import com.epam.edp.demo.enums.BookingState;
import com.epam.edp.demo.enums.Role;
import com.epam.edp.demo.model.Booking;
import com.epam.edp.demo.model.Review;
import com.epam.edp.demo.model.Tour;
import com.epam.edp.demo.model.User;
import com.epam.edp.demo.repository.BookingRepository;
import com.epam.edp.demo.repository.ReviewRepository;
import com.epam.edp.demo.repository.TourRepository;
import com.epam.edp.demo.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Auto-seeds the database on startup if collections are empty.
 * No profile needed — just start the app and data will be inserted if missing.
 */
@Component
public class DataSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final TourRepository tourRepository;
    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.admin-password:}")
    private String adminPassword;

    @Value("${app.seed.agent-password:}")
    private String agentPassword;

    public DataSeeder(UserRepository userRepository,
                      TourRepository tourRepository,
                      ReviewRepository reviewRepository,
                      BookingRepository bookingRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tourRepository = tourRepository;
        this.reviewRepository = reviewRepository;
        this.bookingRepository = bookingRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedAdminIfMissing();
        if (tourRepository.count() == 0) {
            seedAgents();
            seedTours();
            seedReviews();
            seedBookings();
            System.out.println("\u2705 Database seeding complete!");
        } else {
            // Seed bookings separately if they are missing
            if (bookingRepository.count() == 0) {
                seedBookings();
            }
            System.out.println("\u2139\uFE0F  Database already has data \u2014 skipping seed.");
        }
    }

    /** Returns the configured password, or a random one (printed once) when none is configured. */
    private static String resolvePassword(String configured, String label) {
        if (configured != null && !configured.isBlank()) {
            return configured;
        }
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder("Aa1@");
        for (int i = 0; i < 16; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        System.out.println("⚠️  No seed password configured for " + label
                + ". Generated one-time password: " + sb);
        return sb.toString();
    }

    private void seedAdminIfMissing() {
        boolean adminExists = userRepository.findAll().stream()
                .anyMatch(u -> u.getRole() == Role.ADMIN);
        if (!adminExists) {
            User admin = new User("Admin", "User", "admin@mail.com",
                    passwordEncoder.encode(resolvePassword(adminPassword, "admin")), Role.ADMIN);
            userRepository.save(admin);
            System.out.println("\u2705 Admin user created: admin@mail.com");
        }
    }

    // ─────────────────────────────────────────────────────────────
    // AGENTS
    // ─────────────────────────────────────────────────────────────
    private void seedAgents() {
        userRepository.deleteAll(userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.TRAVEL_AGENT).toList());

        String hash = passwordEncoder.encode(resolvePassword(agentPassword, "agents"));

        User agent1 = new User("Tyrone", "Boyer", "agent1@travelagency.com", hash, Role.TRAVEL_AGENT);
        agent1.setId("agent001");
        agent1.setPhone("480-221-1885");
        agent1.setMessengerLink("Messenger");

        User agent2 = new User("Sarah", "Mitchell", "agent2@travelagency.com", hash, Role.TRAVEL_AGENT);
        agent2.setId("agent002");
        agent2.setPhone("312-555-0198");
        agent2.setMessengerLink("WhatsApp");

        User agent3 = new User("Marco", "Rossi", "agent3@travelagency.com", hash, Role.TRAVEL_AGENT);
        agent3.setId("agent003");
        agent3.setPhone("+39-06-555-1234");
        agent3.setMessengerLink("Telegram");

        userRepository.saveAll(List.of(agent1, agent2, agent3));
        System.out.println("✅ Seeded 3 travel agents");
    }

    // ─────────────────────────────────────────────────────────────
    // TOURS
    // ─────────────────────────────────────────────────────────────
    private void seedTours() {
        tourRepository.deleteAll();

        List<Tour> tours = List.of(
                tour("tour001", "Paris City Escape", "Paris",
                        "Discover the romance of Paris with iconic landmarks, world-class cuisine, and breathtaking art. Walk along the Seine, visit the Eiffel Tower, and explore the Louvre.",
                        List.of("https://images.unsplash.com/photo-1502602898657-3e91760cbb34?w=800"),
                        4.8, 142,
                        List.of(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 7, 1), LocalDate.of(2026, 8, 1)),
                        List.of("5 days", "7 days"),
                        Map.of("5 days", "$999", "7 days", "$1299"),
                        List.of("BB", "HB"), Map.of("HB", "$35"),
                        "Cruises", "Hotel Le Marais",
                        "A charming boutique hotel in the heart of Paris near major attractions.",
                        "4-star Hotel",
                        Map.of("Visa Required", "Yes", "Language", "French"),
                        4, 2, 6, 30, 8, 7, "agent001"),

                tour("tour002", "Bali Tropical Retreat", "Bali",
                        "Immerse yourself in Bali's lush landscapes, ancient temples, and vibrant culture. Enjoy surfing, spa treatments, and stunning rice terraces in Ubud.",
                        List.of("https://images.unsplash.com/photo-1537996194471-e657df975ab4?w=800"),
                        4.9, 231,
                        List.of(LocalDate.of(2026, 6, 15), LocalDate.of(2026, 7, 15), LocalDate.of(2026, 9, 1)),
                        List.of("7 days", "10 days"),
                        Map.of("7 days", "$1199", "10 days", "$1599"),
                        List.of("BB", "AI"), Map.of("AI", "$55"),
                        "Resorts", "Ubud Jungle Resort",
                        "Luxury resort nestled in the jungle with private villas and infinity pools.",
                        "5-star Resort",
                        Map.of("Visa Required", "On Arrival", "Language", "Balinese/Indonesian"),
                        2, 2, 4, 20, 5, 10, "agent002"),

                tour("tour003", "Rome & Vatican Tour", "Rome",
                        "Walk through 3,000 years of history in Rome. Visit the Colosseum, Vatican City, Sistine Chapel, and toss a coin in the Trevi Fountain.",
                        List.of("https://images.unsplash.com/photo-1552832230-c0197dd311b5?w=800"),
                        4.7, 189,
                        List.of(LocalDate.of(2026, 5, 20), LocalDate.of(2026, 6, 20), LocalDate.of(2026, 9, 10)),
                        List.of("5 days", "8 days"),
                        Map.of("5 days", "$899", "8 days", "$1350"),
                        List.of("BB", "HB"), Map.of("HB", "$30"),
                        "Cruises", "Palazzo Roma Hotel",
                        "Historic palace converted into a modern hotel near the Pantheon.",
                        "4-star Hotel",
                        Map.of("Visa Required", "Schengen", "Language", "Italian"),
                        4, 2, 6, 25, 12, 5, "agent003"),

                tour("tour004", "Dubai Luxury Experience", "Dubai",
                        "Experience the ultra-modern marvel of Dubai — from Burj Khalifa to desert safaris, luxury malls, and spectacular fountain shows.",
                        List.of("https://images.unsplash.com/photo-1512453979798-5ea266f8880c?w=800"),
                        4.6, 175,
                        List.of(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 1), LocalDate.of(2027, 1, 15)),
                        List.of("5 days", "7 days"),
                        Map.of("5 days", "$1499", "7 days", "$1999"),
                        List.of("BB", "HB", "AI"), Map.of("HB", "$50", "AI", "$90"),
                        "Resorts", "Burj Al Arab Adjacent Suites",
                        "5-star luxury hotel offering panoramic views of the Arabian Gulf.",
                        "5-star Hotel",
                        Map.of("Visa Required", "On Arrival", "Language", "Arabic/English"),
                        4, 2, 6, 20, 3, 14, "agent001"),

                tour("tour005", "Tokyo & Kyoto Discovery", "Tokyo",
                        "Blend the ultra-modern and the ancient — Tokyo's neon-lit streets, Mount Fuji views, and Kyoto's serene temples and geisha districts await you.",
                        List.of("https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?w=800"),
                        4.9, 298,
                        List.of(LocalDate.of(2026, 3, 20), LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 15)),
                        List.of("7 days", "10 days", "14 days"),
                        Map.of("7 days", "$1799", "10 days", "$2399", "14 days", "$3099"),
                        List.of("BB", "HB"), Map.of("HB", "$45"),
                        "Cruises", "Shinjuku Grand Hotel",
                        "Modern hotel in the heart of Tokyo with stunning city views.",
                        "4-star Hotel",
                        Map.of("Visa Required", "Yes", "Language", "Japanese"),
                        4, 2, 6, 22, 15, 14, "agent002"),

                tour("tour006", "Santorini Sunset Cruise", "Santorini",
                        "Experience the world-famous sunsets of Santorini, explore white-washed villages, black sand beaches, and ancient ruins of Akrotiri.",
                        List.of("https://images.unsplash.com/photo-1570077188670-e3a8d69ac5ff?w=800"),
                        4.8, 207,
                        List.of(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 7, 1), LocalDate.of(2026, 8, 15)),
                        List.of("5 days", "7 days"),
                        Map.of("5 days", "$1299", "7 days", "$1699"),
                        List.of("BB", "HB"), Map.of("HB", "$40"),
                        "Resorts", "Caldera View Suites",
                        "Stunning cliffside hotel with infinity pools overlooking the caldera.",
                        "5-star Hotel",
                        Map.of("Visa Required", "Schengen", "Language", "Greek"),
                        2, 1, 3, 18, 10, 10, "agent003"),

                tour("tour007", "New York City Adventure", "New York",
                        "The city that never sleeps — Times Square, Central Park, Statue of Liberty, Broadway shows, and world-class dining await in the Big Apple.",
                        List.of("https://images.unsplash.com/photo-1485871981521-5b1fd3805eee?w=800"),
                        4.6, 312,
                        List.of(LocalDate.of(2026, 6, 10), LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 20)),
                        List.of("5 days", "7 days"),
                        Map.of("5 days", "$1099", "7 days", "$1499"),
                        List.of("RO", "BB"), Map.of("BB", "$25"),
                        "Cruises", "Manhattan Skyline Hotel",
                        "Contemporary hotel in Midtown Manhattan within walking distance of top attractions.",
                        "4-star Hotel",
                        Map.of("Visa Required", "ESTA", "Language", "English"),
                        4, 2, 6, 35, 20, 7, "agent001"),

                tour("tour008", "Maldives Island Paradise", "Maldives",
                        "Escape to the pristine turquoise waters of the Maldives. Overwater bungalows, snorkeling with manta rays, and total serenity.",
                        List.of("https://images.unsplash.com/photo-1573843981267-be1999ff37cd?w=800"),
                        5.0, 95,
                        List.of(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 10, 15), LocalDate.of(2026, 12, 1)),
                        List.of("5 days", "7 days", "10 days"),
                        Map.of("5 days", "$2499", "7 days", "$3299", "10 days", "$4499"),
                        List.of("HB", "FB", "AI"), Map.of("FB", "$60", "AI", "$110"),
                        "Resorts", "Azure Overwater Villas",
                        "Exclusive overwater bungalows with glass floors and direct lagoon access.",
                        "5-star Resort",
                        Map.of("Visa Required", "On Arrival", "Language", "Dhivehi/English"),
                        2, 0, 2, 10, 4, 21, "agent002"),

                tour("tour009", "Safari in Kenya", "Nairobi",
                        "Witness the Great Migration in Masai Mara, spot the Big Five, and experience authentic Maasai culture in Kenya's iconic savannah.",
                        List.of("https://images.unsplash.com/photo-1516426122078-c23e76319801?w=800"),
                        4.9, 118,
                        List.of(LocalDate.of(2026, 7, 15), LocalDate.of(2026, 8, 15), LocalDate.of(2026, 10, 1)),
                        List.of("7 days", "10 days"),
                        Map.of("7 days", "$2299", "10 days", "$3099"),
                        List.of("FB"), Map.of(),
                        "Hikes", "Masai Mara Tented Camp",
                        "Luxury tented camp on the edge of Masai Mara National Reserve.",
                        "Luxury Safari Camp",
                        Map.of("Visa Required", "Yes", "Language", "Swahili/English", "Best Season", "July-October"),
                        6, 2, 8, 16, 6, 30, "agent003"),

                tour("tour010", "Barcelona & Costa Brava", "Barcelona",
                        "Explore Gaudi's masterpieces, stroll La Rambla, relax on Costa Brava's golden beaches, and savour authentic tapas in vibrant Barcelona.",
                        List.of("https://images.unsplash.com/photo-1583422409516-2895a77efded?w=800"),
                        4.7, 223,
                        List.of(LocalDate.of(2026, 5, 15), LocalDate.of(2026, 7, 1), LocalDate.of(2026, 9, 1)),
                        List.of("5 days", "7 days"),
                        Map.of("5 days", "$849", "7 days", "$1149"),
                        List.of("BB", "HB"), Map.of("HB", "$32"),
                        "Resorts", "Hotel Arts Barcelona",
                        "Iconic skyscraper hotel on the beachfront with panoramic Mediterranean views.",
                        "5-star Hotel",
                        Map.of("Visa Required", "Schengen", "Language", "Spanish/Catalan"),
                        4, 2, 6, 28, 11, 7, "agent001"),

                tour("tour011", "Swiss Alps Adventure", "Zurich",
                        "Ski the legendary slopes of Zermatt, cruise Lake Geneva, explore Lucerne's medieval charm and marvel at breathtaking Alpine scenery.",
                        List.of("https://images.unsplash.com/photo-1531973576160-7125cd663d86?w=800"),
                        4.8, 156,
                        List.of(LocalDate.of(2026, 12, 10), LocalDate.of(2027, 1, 10), LocalDate.of(2027, 2, 10)),
                        List.of("5 days", "7 days", "10 days"),
                        Map.of("5 days", "$1799", "7 days", "$2399", "10 days", "$3199"),
                        List.of("HB", "FB"), Map.of("FB", "$55"),
                        "Hikes", "Zermatt Alpine Lodge",
                        "Cozy mountain lodge with Matterhorn views and direct ski-in/ski-out access.",
                        "4-star Chalet Hotel",
                        Map.of("Visa Required", "Schengen", "Language", "German/French/Italian"),
                        4, 2, 6, 20, 7, 14, "agent002"),

                tour("tour012", "Rajasthan Royal Heritage Tour", "Rajasthan",
                        "Travel through the land of maharajas — majestic forts, colourful bazaars, camel rides in the Thar Desert, and regal palace hotels in Jaipur, Jodhpur, and Udaipur.",
                        List.of("https://images.unsplash.com/photo-1524492412937-b28074a5d7da?w=800"),
                        4.7, 184,
                        List.of(LocalDate.of(2026, 10, 15), LocalDate.of(2026, 11, 15), LocalDate.of(2027, 1, 20)),
                        List.of("7 days", "10 days"),
                        Map.of("7 days", "$899", "10 days", "$1249"),
                        List.of("BB", "HB", "FB"), Map.of("HB", "$20", "FB", "$38"),
                        "Hikes", "Umaid Bhawan Palace",
                        "A living palace and one of the world's greatest heritage hotels in Jodhpur.",
                        "Palace Hotel",
                        Map.of("Visa Required", "Yes", "Language", "Hindi/Rajasthani", "Best Season", "Oct-Mar"),
                        6, 3, 9, 24, 9, 10, "agent003")
        );

        tourRepository.saveAll(tours);
        System.out.println("✅ Seeded " + tours.size() + " tours");
    }

    // ─────────────────────────────────────────────────────────────
    // REVIEWS
    // ─────────────────────────────────────────────────────────────
    private void seedReviews() {
        reviewRepository.deleteAll();

        List<Review> reviews = List.of(
                review("rev001", "tour001", "user001", "Emily Carter",     "https://i.pravatar.cc/150?img=1",  5.0, "Absolutely magical! The Eiffel Tower at night took my breath away. Hotel Le Marais was perfectly located.",            LocalDate.of(2026, 4, 10)),
                review("rev002", "tour001", "user002", "James Liu",        "https://i.pravatar.cc/150?img=2",  4.5, "Great trip overall. The guided Louvre tour was a highlight. Would have loved more free time.",                       LocalDate.of(2026, 4, 15)),
                review("rev003", "tour001", "user003", "Sophie Dupont",    "https://i.pravatar.cc/150?img=3",  4.8, "Wonderful experience! Food was amazing and the hotel staff were incredibly helpful.",                               LocalDate.of(2026, 4, 20)),
                review("rev004", "tour002", "user004", "Aisha Patel",      "https://i.pravatar.cc/150?img=4",  5.0, "Bali exceeded every expectation. The infinity pool villa was pure paradise. Will definitely return!",                LocalDate.of(2026, 3, 22)),
                review("rev005", "tour002", "user005", "Michael Torres",   "https://i.pravatar.cc/150?img=5",  4.7, "The rice terraces in Ubud were stunning. All inclusive package was excellent value.",                               LocalDate.of(2026, 3, 28)),
                review("rev006", "tour002", "user006", "Yuki Tanaka",      "https://i.pravatar.cc/150?img=6",  4.9, "A truly transformative trip. The temple visits and spa treatments were incredible.",                                 LocalDate.of(2026, 4, 1)),
                review("rev007", "tour003", "user007", "Marco Bianchi",    "https://i.pravatar.cc/150?img=7",  4.8, "History came alive in Rome! The Vatican Museum skip-the-line access was worth every penny.",                        LocalDate.of(2026, 3, 10)),
                review("rev008", "tour003", "user008", "Laura Hernandez",  "https://i.pravatar.cc/150?img=8",  4.6, "Fantastic tour with knowledgeable guides. The Colosseum visit was unforgettable.",                                  LocalDate.of(2026, 3, 15)),
                review("rev009", "tour003", "user009", "David Kim",        "https://i.pravatar.cc/150?img=9",  4.5, "Great historical experience. Wish there was more time at the Sistine Chapel.",                                      LocalDate.of(2026, 3, 20)),
                review("rev010", "tour004", "user010", "Priya Sharma",     "https://i.pravatar.cc/150?img=10", 4.7, "Dubai is spectacular! The desert safari was the highlight. Burj Khalifa views were breathtaking.",                  LocalDate.of(2026, 2, 14)),
                review("rev011", "tour004", "user011", "Omar Al-Farsi",    "https://i.pravatar.cc/150?img=11", 4.4, "Amazing luxury experience. The hotel was top-notch. Mall of Emirates exceeded expectations.",                       LocalDate.of(2026, 2, 20)),
                review("rev012", "tour005", "user012", "Hana Nakamura",    "https://i.pravatar.cc/150?img=12", 5.0, "Japan is a dream destination and this tour delivered perfectly. Cherry blossoms in Kyoto were magical.",             LocalDate.of(2026, 4, 5)),
                review("rev013", "tour005", "user013", "Chris Walker",     "https://i.pravatar.cc/150?img=13", 4.9, "Best trip of my life! The contrast between Tokyo and Kyoto was incredible. Food was outstanding.",                   LocalDate.of(2026, 4, 8)),
                review("rev014", "tour005", "user014", "Mei Chen",         "https://i.pravatar.cc/150?img=14", 4.8, "The geisha district in Kyoto was unlike anything I have ever seen. Highly recommended!",                            LocalDate.of(2026, 4, 12)),
                review("rev015", "tour006", "user015", "Isabella Rossi",   "https://i.pravatar.cc/150?img=15", 4.9, "The most romantic destination I have ever visited. Sunset from Oia was simply breathtaking.",                       LocalDate.of(2026, 3, 5)),
                review("rev016", "tour006", "user016", "Tom Bradley",      "https://i.pravatar.cc/150?img=16", 4.7, "The caldera views from our room were incredible. Excellent food and wine throughout.",                               LocalDate.of(2026, 3, 10)),
                review("rev017", "tour007", "user017", "Rachel Green",     "https://i.pravatar.cc/150?img=17", 4.5, "NYC is electric! Times Square at night, Central Park at sunrise — both unforgettable.",                            LocalDate.of(2026, 4, 18)),
                review("rev018", "tour007", "user018", "Carlos Mendez",    "https://i.pravatar.cc/150?img=18", 4.3, "Great city tour. The Broadway show included in the package was a wonderful bonus.",                                 LocalDate.of(2026, 4, 22)),
                review("rev019", "tour008", "user019", "Nina Johansson",   "https://i.pravatar.cc/150?img=19", 5.0, "Heaven on Earth. The overwater bungalow with glass floor was the most unique experience of my life.",              LocalDate.of(2026, 3, 30)),
                review("rev020", "tour008", "user020", "Ahmed Hassan",     "https://i.pravatar.cc/150?img=20", 5.0, "Pristine waters, incredible marine life, and world-class service. Absolutely worth every penny.",                   LocalDate.of(2026, 4, 2)),
                review("rev021", "tour009", "user021", "Grace Okonkwo",    "https://i.pravatar.cc/150?img=21", 5.0, "Witnessing the Great Migration was a life-changing experience. Our guide was exceptional.",                         LocalDate.of(2026, 2, 28)),
                review("rev022", "tour009", "user022", "Peter van der Berg","https://i.pravatar.cc/150?img=22",4.8, "Spotted all Big Five on day two! The tented camp was surprisingly luxurious.",                                      LocalDate.of(2026, 3, 4)),
                review("rev023", "tour010", "user023", "Elena Fernandez",  "https://i.pravatar.cc/150?img=23", 4.8, "Gaudi's architecture is truly one-of-a-kind. The tapas tour through the Gothic Quarter was delicious!",             LocalDate.of(2026, 4, 14)),
                review("rev024", "tour010", "user024", "Alex Murphy",      "https://i.pravatar.cc/150?img=24", 4.6, "Barcelona has incredible energy. Sagrada Familia left me speechless. Great beach time too.",                        LocalDate.of(2026, 4, 19)),
                review("rev025", "tour011", "user025", "Lena Muller",      "https://i.pravatar.cc/150?img=25", 4.9, "The Matterhorn views from Zermatt were jaw-dropping. Ski slopes were perfectly groomed.",                          LocalDate.of(2026, 1, 20)),
                review("rev026", "tour011", "user026", "John Stevens",     "https://i.pravatar.cc/150?img=26", 4.7, "Switzerland in winter is a fairytale. Lake Geneva cruise and Lucerne were absolute highlights.",                    LocalDate.of(2026, 1, 25)),
                review("rev027", "tour012", "user027", "Ananya Gupta",     "https://i.pravatar.cc/150?img=27", 4.8, "The palace hotels were spectacular. Camel ride in Thar Desert at sunset was unforgettable.",                       LocalDate.of(2026, 2, 10)),
                review("rev028", "tour012", "user028", "Robert Clark",     "https://i.pravatar.cc/150?img=28", 4.6, "Rich culture, stunning architecture, and warm hospitality. Udaipur was the crown jewel.",                          LocalDate.of(2026, 2, 15))
        );

        reviewRepository.saveAll(reviews);
        System.out.println("✅ Seeded " + reviews.size() + " reviews");
    }

    // ─────────────────────────────────────────────────────────────
    // BOOKINGS
    // ─────────────────────────────────────────────────────────────
    private void seedBookings() {
        bookingRepository.deleteAll();

        // agent001 -> tour001 (Paris), tour004 (Dubai), tour007 (New York), tour010 (Barcelona)
        // agent002 -> tour002 (Bali),  tour005 (Tokyo), tour008 (Maldives), tour011 (Swiss Alps)
        // agent003 -> tour003 (Rome),  tour006 (Santorini), tour009 (Kenya), tour012 (Rajasthan)
        String[][] agentTours = {
            {"tour001", "tour004", "tour007", "tour010"},
            {"tour002", "tour005", "tour008", "tour011"},
            {"tour003", "tour006", "tour009", "tour012"},
        };
        String[][] tourNames = {
            {"Paris City Escape", "Dubai Luxury Experience", "New York City Adventure", "Barcelona & Costa Brava"},
            {"Bali Tropical Retreat", "Tokyo & Kyoto Discovery", "Maldives Island Paradise", "Swiss Alps Adventure"},
            {"Rome & Vatican Tour", "Santorini Sunset Cruise", "Safari in Kenya", "Rajasthan Royal Heritage Tour"},
        };
        String[][] destinations = {
            {"Paris", "Dubai", "New York", "Barcelona"},
            {"Bali", "Tokyo", "Maldives", "Zurich"},
            {"Rome", "Santorini", "Nairobi", "Rajasthan"},
        };

        List<Booking> bookings = new ArrayList<>();
        int num = 1;

        // Seed 8 bookings per agent per month, Jan 2025 – May 2026
        for (int year = 2025; year <= 2026; year++) {
            int maxMonth = (year == 2026) ? 5 : 12;
            for (int month = 1; month <= maxMonth; month++) {
                for (int agentIdx = 0; agentIdx < 3; agentIdx++) {
                    for (int i = 0; i < 8; i++) {
                        int tourIdx = i % 4;
                        int day = Math.min(1 + i * 3, 28); // days 1,4,7,10,13,16,19,22
                        Booking b = new Booking();
                        b.setId(String.format("book%04d", num++));
                        b.setUserId(String.format("user%03d", ((num % 28) + 1)));
                        b.setTourId(agentTours[agentIdx][tourIdx]);
                        b.setDate(LocalDate.of(year, month, day));
                        b.setDuration("7 days");
                        b.setMealPlan("BB");
                        b.setAdults(2);
                        b.setChildren(0);
                        b.setState(BookingState.BOOKED);
                        b.setTotalPrice("$999");
                        b.setTourName(tourNames[agentIdx][tourIdx]);
                        b.setDestination(destinations[agentIdx][tourIdx]);
                        bookings.add(b);
                    }
                }
            }
        }

        bookingRepository.saveAll(bookings);
        System.out.println("\u2705 Seeded " + bookings.size() + " bookings");
    }

    // ─────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────
    private Tour tour(String id, String name, String destination, String summary,
                      List<String> imageUrls, double rating, int reviewCount,
                      List<LocalDate> startDates, List<String> durations,
                      Map<String, String> pricePerDuration,
                      List<String> mealPlans, Map<String, String> mealSupplementsPerDay,
                      String tourType, String hotelName, String hotelDescription,
                      String accommodation, Map<String, String> customDetails,
                      int adultsMax, int childrenMax, int totalMax,
                      int totalCapacity, int bookedCount, int freeCancelDays,
                      String agentId) {
        Tour t = new Tour();
        t.setId(id);
        t.setName(name);
        t.setDestination(destination);
        t.setSummary(summary);
        t.setImageUrls(imageUrls);
        t.setRating(rating);
        t.setReviewCount(reviewCount);
        t.setStartDates(startDates);
        t.setDurations(durations);
        t.setPricePerDuration(pricePerDuration);
        t.setMealPlans(mealPlans);
        t.setMealSupplementsPerDay(mealSupplementsPerDay);
        t.setTourType(tourType);
        t.setHotelName(hotelName);
        t.setHotelDescription(hotelDescription);
        t.setAccommodation(accommodation);
        t.setCustomDetails(customDetails);
        Tour.GuestQuantity gq = new Tour.GuestQuantity();
        gq.setAdultsMaxValue(adultsMax);
        gq.setChildrenMaxValue(childrenMax);
        gq.setTotalMaxValue(totalMax);
        t.setGuestQuantity(gq);
        t.setTotalCapacity(totalCapacity);
        t.setBookedCount(bookedCount);
        t.setFreeCancellationDaysBefore(freeCancelDays);
        t.setAssignedAgentId(agentId);
        return t;
    }

    private Review review(String id, String tourId, String userId, String userName,
                          String avatarUrl, double rate, String comment, LocalDate reviewDate) {
        Review r = new Review();
        r.setId(id);
        r.setTourId(tourId);
        r.setUserId(userId);
        r.setUserName(userName);
        r.setUserAvatarUrl(avatarUrl);
        r.setRate(rate);
        r.setComment(comment);
        r.setReviewDate(reviewDate);
        return r;
    }
}

