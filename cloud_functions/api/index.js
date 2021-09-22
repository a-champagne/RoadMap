const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();
const express = require('express');
const app = express();
const db = admin.database();

// AUTHENTICATION MIDDLEWARE

const authenticate = async (req, res, next) => {
  if (!req.headers.authtoken) {
    res.status(403).json({error: "unauthorized"});
    return;
  }
  const idToken = req.headers.authtoken;
  try {
    const decodedIdToken = await admin.auth().verifyIdToken(idToken);
    req.user = decodedIdToken;
    next();
    return;
  } catch(e) {
    res.status(403).json({error: "unauthorized"});
    return;
  }
};

// Middleware for every request.
//app.use(authenticate);

var get_user_metadata = async (id) => {
  var snapshot = await db
    .ref(`/Users/${id}/`)
	.once('value');

  return snapshot.val();
}

app.get('/user_type/:id', async (req, res) => {
  var meta = await get_user_metadata(req.params.id);
  
  res.status(200).json({
    "user_id": req.params.id,
    "user_type": meta.user_type
  });
});

// TRIP ENDPOINTS

var get_trip = async (id) => {
  var snapshot = await db
    .ref(`/Trips/${id}/`)
	.once('value');

  return snapshot.val();
};

app.post('/trip/:id/zone/', async (req, res) => {
    var zone = req.body;

    var trip = await get_trip(req.params.id);

    if (!trip) {
      console.log("Missing trip.");
      res.status(400).json({
        error: `Trip ID ${req.params.id} not found.`
      });
      return;
    }

    await db.ref(`/Trips/${req.params.id}/Zone/`)
    .set(zone);

    res.status(200).json(zone);
});

app.post('/trip/:id/message/', async (req, res) => {
  var timestamp = Date.now();
  var trip = await get_trip(req.params.id);

  if (!trip) {
    res.status(400).json({
      error: `Trip ID ${req.params.id} not found.`
    });
    return;
  }

  await db.ref(`/Trips/${req.params.id}/messages/${timestamp}`)
  .set(req.body);

  res.status(200).json({message_timestamp: timestamp});
});

app.get('/trip/:id/message/all', async (req, res) => {

  if (!get_trip(req.params.id)) {
      res.status(400).json({
          error: `Trip ID ${req.params.id} not found.`
      });
      return;
  }

  var messagesRef = await db.ref(`/Trips/${req.params.id}/messages`).once('value');
  var messages = messagesRef.val() || {};

  res.status(200).json(messages);
});

app.get('/trip/:id/message/since=:timestamp', async (req, res) => {
  var timestamp = parseInt(req.params.timestamp);

  if (!timestamp) {
      res.status(400).json({error: "Missing valid timestamp param."});
      return;
  }

  if (!get_trip(req.params.id)) {
      res.status(400).json({
          error: `Trip ID ${req.params.id} not found.`
      });
      return;
  }

  var messagesRef = await db.ref(`/Trips/${req.params.id}/messages`).once('value');
  var messages = messagesRef.val() || {};

  var filtered = {};
  Objects.keys(messages).forEach(ts => {
      if (timestamp < parseInt(ts)) {
          filtered[ts] = messages[ts];
      }
  });

  res.status(200).json(filtered);
});

app.post('/trip/', async (req, res) => {
  
  var tripId = -1;
  
  await db.ref('/next_trip_id/').transaction((val) => {
      if (val) {
          tripId = val + 1;
          return tripId;
      }
      return val;
  });
  
  if (tripId == -1) {
    res.status(500).json({error:"Invalid trip id"});
  }

  var trip = {
    id: tripId,
    active: false,
    driverID: "NO_DRIVER",
    driverName: "NO_DRIVER",
    speed: 0,
    messages: {},
    vehicleID: "UNASSIGNED",
    finish: -1
  };

  const destination = req.body.Destination;
  const origin = req.body.Origin;
  const timestamp = req.body.TimeStamp;
  const active = req.body.active;
  const driverID = req.body.driverID;
  const driverName = req.body.driverName;
  const vehicleID = req.body.vehicleID;

  console.log(req.body);

  if (!origin || !origin.address || isNaN(origin.latitude) || isNaN(origin.longitude)) {
    res.status(400).json({"error": "Missing origin."});
    console.log("missing origin: " + origin);
    return;
  }

  if (!destination || !destination.address || isNaN(destination.latitude) || isNaN(destination.longitude)) {
    res.status(400).send({"error": "Missing destination."});
    console.log("missing dest: " + destination);
    return;
  }

  if (!timestamp) {
    res.status(400).json({
      error: "Missing timestamp."
    });
    console.log("Missing timestamp: " + timestamp);
    return;
  }

  trip.Destination = {
    address: destination.address,
    latitude: destination.latitude,
    longitude: destination.longitude
  }

  trip.Origin = {
    address: origin.address,
    latitude: origin.latitude,
    longitude: origin.longitude
  }

  trip.TimeStamp = timestamp;

  if (active === true) {
    trip.active = true;
  }

  if (driverID !== undefined) {
    trip.driverID = driverID;
  }

  if (driverName !== undefined) {
    trip.driverName = driverName;
  }

  if (vehicleID !== undefined) {
    trip.vehicleID = vehicleID;
  }

  await db
    .ref(`/Trips/${tripId}/`)
    .set(trip);

  res.status(200).json(trip);
});

app.put('/trip/:id', async (req, res) => {
  try {
    db
    .ref(`/Trips/${req.params.id}/`)
    .once('value', (snapshot) => {
      console.log(snapshot.val());
      if (snapshot.val() === null) {
        console.log(`Cannot find trip ${id}. Snapshot: ${snapshot}`);
        return res.status(404).json({error: "Not found"});
      } else {

        var trip = snapshot.val();

        const destination = req.body.Destination;
        const origin = req.body.Origin;
        const timestamp = req.body.TimeStamp;
        const active = req.body.active;
        const driverID = req.body.driverID;
        const driverName = req.body.driverName;
        const vehicleID = req.body.vehicleID;

        if (origin !== undefined && origin.address !== undefined) {
          trip.Origin.address = origin.address;
        }
        if (origin !== undefined && origin.latitude !== undefined) {
          trip.Origin.latitude = origin.latitude;
        }
        if (origin !== undefined && origin.longitude !== undefined) {
          trip.Origin.longitude = origin.longitude;
        }

        if (destination !== undefined && destination.address !== undefined) {
          trip.Destination.address = destination.address;
        }
        if (destination !== undefined && destination.latitude !== undefined) {
          trip.Destination.latitude = destination.latitude;
        }
        if (destination !== undefined && destination.longitude !== undefined) {
          trip.Destination.longitude = destination.longitude;
        }
        if (timestamp !== undefined) trip.TimeStamp = timestamp;
        if (active !== undefined) trip.active = active;
        if (driverID !== undefined) trip.driverID = driverID;
        if (driverName !== undefined) trip.driverName = driverName;
        if (vehicleID !== undefined) trip.vehicleID = vehicleID;

        db
          .ref(`/Trips/${req.params.id}/`)
          .set(trip)
          .then(() => res.status(200).json(trip));
        console.log("update: " + trip);
      }
    });
  } catch(error) {
      console.log(error)
      return res.status(404).json({error: "Not found"});
  }
});

app.get('/trip/', async (req, res) => {
  var trips = await db.ref("/Trips/")
    .once('value')
  
  res.status(200).json(trips);
});

app.get('/trip/:id', async (req, res) => {
  try {
    db
      .ref(`/Trips/${req.params.id}/`)
      .once('value', (snapshot) => {
        console.log(snapshot.val());
        if (snapshot.val() === null) {
          return res.status(404).json({error: "Not found"});
        } else return res.status(200).json(snapshot.val());
    });
  } catch(error) {
    console.log(error);
    return res.status(404).json({error: "Not found"});
  }
});

app.delete('/trip/:id', async (req, res) => {
  try {
    db
      .ref(`/Trips/${req.params.id}/`)
      .remove()
      .then(() => res.status(200).json({status:"success"}));
  } catch(error) {
    console.log(error);
    return res.status(404).json({error: "Not found"});
  }
});

app.put('/trip/end/:id', async (req, res) => {
  db
    .ref(`/Trips/${req.params.id}/`)
    .update({finish: Date.now()})
    .then(() => res.status(200).json({status: "success"}));
});

// VEHICLE ENDPOINTS

var get_vehicle = async (id) => {
  var snapshot = await db
    .ref(`/Vehicles/${id}/`)
	.once('value');

  return snapshot.val();
};

app.post('/vehicle/', async (req, res) => {
  var vehicle = {
    Available: false,
    Status: "active",
    DriverID: "NO_DRIVER",
    TripID: "NONE",
    speed: 0,
    Location_Log: {},
  };

  const location = req.body.Location;
  const vehicleID = req.body.VehicleID;

  console.log(req.body);

  if (!location || isNaN(location.latitude) || isNaN(location.longitude)) {
    res.status(400).json({"error": "Missing location."});
    console.log("missing location fields: " + location);
    return;
  }

  if (!vehicleID) {
    res.status(400).json({
      error: "Missing vehicleID."
    });
    console.log("Missing vehicleID: " + vehicleID);
    return;
  }

  var existingVehicle = await get_vehicle(vehicleID);

  if (existingVehicle !== null) {
    console.log(`Existing vehicle - ID: ${vehicleID}`);
    console.log(existingVehicle);
    return res.status(400).json({error: "Already exists"});
  }

  vehicle.Location = {
    latitude: location.latitude,
    longitude: location.longitude
  }

  vehicle.vehicleID = vehicleID;

  await db
    .ref(`/Vehicles/${vehicleID}/`)
    .set(vehicle);

  res.status(200).json(vehicle);
});

app.put('/vehicle/:id', async (req, res) => {
  try {
    db
    .ref(`/Vehicles/${req.params.id}/`)
    .once('value', (snapshot) => {
      console.log(snapshot.val());
      if (snapshot.val() === null) {
        console.log(`Cannot find vehicle ${id}. Snapshot: ${snapshot}`);
        return res.status(404).json({error: "Not found"});
      } else {

        var vehicle = snapshot.val();

        const location = req.body.Location;
        const status = req.body.status;
        const driverID = req.body.driverID;
        const vehicleID = req.body.vehicleID;
        const available = req.body.available;

        if (location !== undefined && location.address !== undefined) {
          vehicle.Location.address = location.address;
        }
        if (location !== undefined && location.latitude !== undefined) {
          vehicle.Location.latitude = location.latitude;
        }
        if (location !== undefined && location.longitude !== undefined) {
          vehicle.Location.longitude = location.longitude;
        }

        if (status !== undefined) vehicle.Status = status;

        if (available !== undefined) vehicle.Available = available;

		if (driverID !== undefined) vehicle.DriverID = driverID;

        db
          .ref(`/Vehicles/${req.params.id}/`)
          .set(vehicle)
          .then(() => res.status(200).json(vehicle));
        console.log("update: " + vehicle);
      }
    });
  } catch(error) {
      console.log(error)
      return res.status(404).json({error: "Not found"});
  }
});

app.get('/vehicle/:id', async (req, res) => {
  try {
    db
      .ref(`/Vehicles/${req.params.id}/`)
      .once('value', (snapshot) => {
        console.log(snapshot.val());
        if (snapshot.val() === null) {
          return res.status(404).json({error: "Not found"});
        } else return res.status(200).json(snapshot.val());
    });
  } catch(error) {
    console.log(error);
    return res.status(404).json({error: "Not found"});
  }
});

app.get('/vehicle/', async (req, res) => {
  var vehicles = await db.ref(`/Vehicles/`)
    .once('value');
  
  res.status(200).json(vehicles);
});

app.delete('/vehicle/:id', async (req, res) => {
  try {
    db
      .ref(`/Vehicles/${req.params.id}/`)
      .remove()
      .then(() => res.status(200).json({status:"success"}));
  } catch(error) {
    console.log(error);
    return res.status(404).json({error: "Not found"});
  }
});

// TODO
app.post('/vehicle/:id/report_location/lat=:lat&lon=:lon&speed=:speed', async (req, res) => {

    var timestamp = Date.now() / 1000;

    await db
      .ref(`/Vehicles/${req.params.id}/Location_Log/${timestamp}/`)
      .set({latitude: req.params.lat, longitude: req.params.lon, speed: req.params.speed});

    res.status(200).json({status:"success", "timestamp_s":timestamp});
});

exports.api = functions.https.onRequest(app);
