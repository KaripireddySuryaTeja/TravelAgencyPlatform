const db = connect(process.env.MONGODB_URI || "mongodb://localhost:27017/travelagency");

// Seed customer users referenced by bookings (user001-user028)
const firstNames = [
  "James", "Maria", "Robert", "Jennifer", "Michael", "Linda", "David", "Elizabeth",
  "William", "Barbara", "Richard", "Susan", "Joseph", "Jessica", "Thomas", "Sarah",
  "Charles", "Karen", "Christopher", "Nancy", "Daniel", "Lisa", "Matthew", "Betty",
  "Anthony", "Margaret", "Mark", "Sandra"
];
const lastNames = [
  "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis",
  "Rodriguez", "Martinez", "Hernandez", "Lopez", "Gonzalez", "Wilson", "Anderson",
  "Thomas", "Taylor", "Moore", "Jackson", "Martin", "Lee", "Perez", "Thompson",
  "White", "Harris", "Sanchez", "Clark", "Ramirez"
];

const customers = [];
for (let i = 1; i <= 28; i++) {
  const id = "user" + String(i).padStart(3, "0");
  customers.push({
    _id: id,
    firstName: firstNames[i - 1],
    lastName: lastNames[i - 1],
    email: `${firstNames[i - 1].toLowerCase()}.${lastNames[i - 1].toLowerCase()}@example.com`,
    passwordHash: "$2a$12$dummyhashforseeding1234567890abcdef",
    role: "CUSTOMER",
    phone: `+1-555-${String(100 + i).padStart(4, "0")}`,
    failedLoginAttempts: 0,
    lockedUntil: null,
    createdAt: new Date(),
    updatedAt: new Date()
  });
}

// Only insert if they don't already exist
customers.forEach(c => {
  db.users.updateOne({ _id: c._id }, { $setOnInsert: c }, { upsert: true });
});

print(`✓ Seeded ${customers.length} customer users (user001-user028)`);
