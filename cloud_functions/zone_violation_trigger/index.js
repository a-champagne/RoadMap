const nodemailer = require('nodemailer');
const functions = require('firebase-functions');

const gmailEmail = config.gmail;
const gmailPassword = config.password;
const toEmail = config.recipient;
const mailTransport = nodemailer.createTransport({
  service: 'gmail',
  auth: {
    user: gmailEmail,
    pass: gmailPassword,
  },
});

/**
 * Triggered by a change to a Firebase RTDB reference.
 *
 * @param {!Object} event Event payload and metadata.
 * @param {!Function} callback Callback function to signal completion.
 */
exports.zoneviolation = (event, callback) => {
  const triggerResource = event.resource;
  console.log('Function triggered by change to: ' +  triggerResource);
  console.log(JSON.stringify(event));
  
  // Notify Admin
  // Email Admin
  const mailOptions = {
    from: "RoadMap Team <noreply@firebase.com>",
    to: toEmail,
    subject : "New Zone Violation Detected",
    text : "A new zone violation has been detected. Check Firebase for details.\n\n- RoadMap Team"
  };
  mailTransport.sendMail(mailOptions).then(() => console.log("Email Sent"));
  
  callback();
};
