// Seeds the local "weather.observations" collection with realistic random data
// for the last 10 days (30-minute intervals). Overwrites existing data in that range.
//
// Usage (mongo 4.4 shell inside the docker-compose container):
//   docker exec -i mongo_db mongo --quiet -u mongo -p mongo --authenticationDatabase admin < seed-local-data.js
// or via maven profile:
//   mvn clean package azure-functions:run -Pseed-local

var DAYS = 10;
var INTERVAL_MINUTES = 30;

var weatherDb = db.getSiblingDB("weather");
var collection = weatherDb.observations;

var now = new Date();
var from = new Date(now.getTime() - DAYS * 24 * 60 * 60 * 1000);

var removed = collection.remove({ dateTime: { $gte: from } });
print("Removed existing docs in range: " + removed.nRemoved);

// July-like weather: daily mean drifts between ~17 and ~23 C,
// diurnal cycle with minimum around 04:00 and maximum around 15:00 UTC.
var dailyMean = 19 + Math.random() * 3;
var pressure = 1012 + (Math.random() - 0.5) * 10;

function round1(x) {
    return Math.round(x * 10) / 10;
}

var docs = [];
for (var t = from.getTime(); t <= now.getTime(); t += INTERVAL_MINUTES * 60 * 1000) {

    var date = new Date(t);
    var hourOfDay = date.getUTCHours() + date.getUTCMinutes() / 60;

    // slow random drift of the daily mean temperature, kept within 15..25 C
    dailyMean += (Math.random() - 0.5) * 0.12;
    dailyMean = Math.min(25, Math.max(15, dailyMean));

    // diurnal sinusoid: min ~04:00, max ~15:00 (approx), amplitude ~5 C
    var diurnal = 5 * Math.sin((hourOfDay - 9.5) / 24 * 2 * Math.PI);
    var outside = dailyMean + diurnal + (Math.random() - 0.5) * 1.2;

    // inside follows outside weakly, stays around 22..25 C
    var inside = 23.5 + (outside - dailyMean) * 0.15 + (Math.random() - 0.5) * 0.6;

    // pressure random walk within 995..1030 hPa
    pressure += (Math.random() - 0.5) * 0.8;
    pressure = Math.min(1030, Math.max(995, pressure));

    docs.push({
        dateTime: date,
        outsideTemperature: round1(outside),
        insideTemperature: round1(inside),
        pressureHpa: round1(pressure)
    });
}

var result = collection.insertMany(docs);
print("Inserted " + result.insertedIds.length + " observations from " + from.toISOString() + " to " + now.toISOString());
