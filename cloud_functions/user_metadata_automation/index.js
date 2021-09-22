const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp({
  "databaseURL":"https://cpen391-test.firebaseio.com/",
  "storageBucket":"undefined",
  "projectId":"cpen391-test"
});

/**
 * Triggered by a change to a Firebase Auth user object.
 *
 * @param {!Object} event Event payload and metadata.
 * @param {!Function} callback Callback function to signal completion.
 */
exports.create_user_metadata = (event, callback) => {
  console.log(JSON.stringify(event));
  
  console.log("Config: " + process.env.FIREBASE_CONFIG);
  
  admin.database().ref(`/Users/${event.data.uid}`).set({
    user_type: 'driver' 
  });
  
  callback();
};
