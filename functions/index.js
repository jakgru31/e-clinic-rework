const { onSchedule } = require('firebase-functions/v2/scheduler');
const { onDocumentCreated } = require('firebase-functions/v2/firestore');
const { initializeApp } = require('firebase-admin/app');
const { getFirestore, Timestamp } = require('firebase-admin/firestore');
const { getMessaging } = require('firebase-admin/messaging');
const { logger } = require('firebase-functions');

initializeApp();
const db = getFirestore();

// 🔔 Function 1: Reminder for appointments not finished
exports.sendAppointmentReminders = onSchedule(
  {
    schedule: 'every 2 minutes',
    timeZone: 'Europe/Warsaw',
    maxInstances: 1,
  },
  async () => {
    try {
      const now = Timestamp.now();
      const twoMinutesAgo = Timestamp.fromMillis(now.toMillis() - 120000);

      const querySnapshot = await db.collection('appointments')
        .where('status', '==', 'NOT_FINISHED')
        .where('date', '<=', twoMinutesAgo)
        .get();

      if (querySnapshot.empty) {
        logger.log('No unfinished appointments found');
        return;
      }

      const promises = querySnapshot.docs.map(async (doc) => {
        try {
          const appointment = doc.data();
          const doctorRef = db.collection('doctors').doc(appointment.doctor_id);
          const doctorSnap = await doctorRef.get();

          if (!doctorSnap.exists || !doctorSnap.data()?.fcmToken) {
            throw new Error(`Doctor ${appointment.doctor_id} has no FCM token`);
          }

          const message = {
            token: doctorSnap.data().fcmToken,
            notification: {
              title: 'Appointment Not Finished',
              body: 'Please confirm the appointment is completed'
            },
            data: {
              type: 'finish_reminder',
              appointmentId: doc.id
            },
            android: {
              priority: 'high'
            }
          };

          await getMessaging().send(message);
          logger.log(`Reminder sent for appointment ${doc.id}`);
        } catch (error) {
          logger.error(`Error processing appointment ${doc.id}:`, error);
        }
      });

      await Promise.all(promises);
    } catch (error) {
      logger.error('Error in sendAppointmentReminders:', error);
    }
  }
);

// 🔔 Function 2: Notify doctor when future appointment is canceled
exports.notifyDoctorOfCanceledFutureAppointment = onSchedule(
  {
    schedule: 'every 1 minutes',
    timeZone: 'Europe/Warsaw',
    maxInstances: 1,
  },
  async () => {
    try {
      const now = Timestamp.now();

      const snapshot = await db.collection('appointments')
        .where('status', '==', 'CANCELED')
        .where('date', '>', now)
        .get();

      if (snapshot.empty) {
        logger.log('No future canceled appointments found');
        return;
      }

      const toNotify = snapshot.docs.filter(doc => !doc.data().hasOwnProperty('notified'));

      if (toNotify.length === 0) {
        logger.log('No new canceled appointments to notify');
        return;
      }

      const promises = toNotify.map(async (doc) => {
        try {
          const appointment = doc.data();

          const doctorSnap = await db.collection('doctors').doc(appointment.doctor_id).get();
          if (!doctorSnap.exists || !doctorSnap.data()?.fcmToken) {
            throw new Error(`Doctor ${appointment.doctor_id} has no FCM token`);
          }

          const userSnap = await db.collection('users').doc(appointment.user_id).get();
          const userName = userSnap.exists ? userSnap.data().name || 'Unknown Patient' : 'Unknown Patient';

          const appointmentDate = appointment.date.toDate().toLocaleString('pl-PL', {
            timeZone: 'Europe/Warsaw',
            day: '2-digit',
            month: '2-digit',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
          });

          const message = {
            token: doctorSnap.data().fcmToken,
            notification: {
              title: 'Appointment Canceled',
              body: `Appointment with ${userName} on ${appointmentDate} has been canceled.`
            },
            data: {
              type: 'cancel_notification',
              appointmentId: doc.id,
              patientName: userName,
              appointmentDate: appointmentDate,
              appointmentTimestamp: String(appointment.date.toMillis())
            },
            android: {
              priority: 'high'
            }
          };

          await getMessaging().send(message);
          await doc.ref.update({ notified: true });
          logger.log(`Doctor notified about canceled appointment ${doc.id}`);
        } catch (error) {
          logger.error(`Failed to notify for appointment ${doc.id}:`, error);
        }
      });

      await Promise.all(promises);
    } catch (error) {
      logger.error('Error in notifyDoctorOfCanceledFutureAppointment:', error);
    }
  }
);

// 🔔 Function 3: Notify patients for appointments today/tomorrow
exports.sendUserAppointmentReminders = onSchedule(
  {
    schedule: 'every 2 minutes',
    timeZone: 'Europe/Warsaw',
    maxInstances: 1,
  },
  async () => {
    try {
      const now = new Date();
      const todayStart = new Date(now);
      todayStart.setHours(0, 0, 0, 0);
      const todayEnd = new Date(now);
      todayEnd.setHours(23, 59, 59, 999);

      const tomorrowStart = new Date(todayStart);
      tomorrowStart.setDate(tomorrowStart.getDate() + 1);
      const tomorrowEnd = new Date(todayEnd);
      tomorrowEnd.setDate(tomorrowEnd.getDate() + 1);

      const todayStartTS = Timestamp.fromDate(todayStart);
      const todayEndTS = Timestamp.fromDate(todayEnd);
      const tomorrowStartTS = Timestamp.fromDate(tomorrowStart);
      const tomorrowEndTS = Timestamp.fromDate(tomorrowEnd);

      const process = async (startTS, endTS, fieldName, label) => {
        const snapshot = await db.collection('appointments')
          .where('date', '>=', startTS)
          .where('date', '<=', endTS)
          .get();

        const filteredDocs = snapshot.docs.filter(doc => !doc.data().hasOwnProperty(fieldName));

        await Promise.all(filteredDocs.map(async (doc) => {
          const appointment = doc.data();
          const userSnap = await db.collection('users').doc(appointment.user_id).get();

          if (!userSnap.exists || !userSnap.data()?.fcmToken) {
            logger.warn(`User ${appointment.user_id} has no FCM token`);
            return;
          }

          const appointmentDate = appointment.date.toDate().toLocaleString('pl-PL', {
            timeZone: 'Europe/Warsaw',
            day: '2-digit',
            month: '2-digit',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
          });

          const message = {
            token: userSnap.data().fcmToken,
            notification: {
              title: `🩺 Reminder: ${label} Appointment`,
              body: `Your appointment is scheduled on ${appointmentDate}`,
            },
            data: {
              type: 'user_appointment_reminder',
              appointmentId: doc.id,
              appointmentDate,
              appointmentTimestamp: String(appointment.date.toMillis()),
              dayType: label,
            },
            android: {
              priority: 'high',
            },
          };

          await getMessaging().send(message);
          await doc.ref.update({ [fieldName]: true });
          logger.log(`Sent ${label} reminder to user ${appointment.user_id} for appointment ${doc.id}`);
        }));
      };

      await process(todayStartTS, todayEndTS, 'reminderTodaySent', 'Today');
      await process(tomorrowStartTS, tomorrowEndTS, 'reminderTomorrowSent', 'Tomorrow');
    } catch (error) {
      logger.error('Error in sendUserAppointmentReminders:', error);
    }
  }
);


// 🔔 Function 4: Remove all old timeslots
exports.cleanupOldTimeSlots = onSchedule(
  {
    //schedule: 'every day 01:00', // Run once daily
    schedule: 'every 2 minutes', // Run once daily
    timeZone: 'Europe/Warsaw',
    maxInstances: 1,
  },
  async () => {
    try {
      const now = new Date();
      now.setHours(0, 0, 0, 0); // Midnight today
      const todayTimestamp = Timestamp.fromDate(now);

      const snapshot = await db.collection('timeslots').get();
      if (snapshot.empty) {
        logger.log('No timeslots found');
        return;
      }

      const cleanupPromises = snapshot.docs.map(async (doc) => {
        const data = doc.data();
        const currentSlots = data.available_slots || [];

        // Filter out old slots
        const futureSlots = currentSlots.filter(ts => ts.toMillis() >= todayTimestamp.toMillis());

        if (futureSlots.length === 0) {
          // All slots are old, delete the document
          await doc.ref.delete();
          logger.log(`Deleted timeslot document ${doc.id} (doctor_id: ${data.doctor_id})`);
        } else {
          logger.log(`Kept document ${doc.id} — has ${futureSlots.length} future slots`);
        }
      });

      await Promise.all(cleanupPromises);
      logger.log(' Finished cleaning up old timeslot documents');
    } catch (error) {
      logger.error(' Error cleaning timeslots:', error);
    }
  }
);

// 🔔 Function 5: Notify recipient when a new chat message is sent
exports.sendChatNotification = onDocumentCreated('chats/{chatId}/messages/{messageId}', async (event) => {
  const messageData = event.data?.data();
  if (!messageData) return;

  const chatId = event.params.chatId;
  const senderId = messageData.sender_id;
  const text = messageData.text || (messageData.image_url ? '📷 Photo' : 'Sent a message');

  try {
    // 1. Get the parent chat to find the participants and their names
    const chatDoc = await db.collection('chats').doc(chatId).get();
    if (!chatDoc.exists) return;

    const chatData = chatDoc.data();
    const participants = chatData.participants || [];
    // The recipient is the other participant
    const recipientId = participants.find(id => id !== senderId);
    if (!recipientId) return;

    // Determine the sender's actual display name (Name + Surname)
    let senderName = messageData.sender_name;
    if (senderId === chatData.doctor_id && chatData.doctor_name) {
      senderName = chatData.doctor_name;
    } else if (senderId === chatData.user_id && chatData.user_name) {
      senderName = chatData.user_name;
    }

    // If still missing, contains '@', or has a period/email format, fetch from Firestore directly
    if (!senderName || senderName === 'User' || senderName.includes('@') || senderName.includes('.')) {
      const doctorDoc = await db.collection('doctors').doc(senderId).get();
      if (doctorDoc.exists) {
        const d = doctorDoc.data();
        senderName = `Dr. ${d.name || ''} ${d.surname || ''}`.trim();
      } else {
        const userDoc = await db.collection('users').doc(senderId).get();
        if (userDoc.exists) {
          const u = userDoc.data();
          senderName = `${u.name || ''} ${u.surname || ''}`.trim();
        }
      }
    }
    if (!senderName) senderName = 'New Message';

    // 2. Lookup recipient's FCM token (check users, then doctors)
    let fcmToken = null;
    let isDoctor = false;

    const userDoc = await db.collection('users').doc(recipientId).get();
    if (userDoc.exists && userDoc.data()?.fcmToken) {
      fcmToken = userDoc.data().fcmToken;
    } else {
      const doctorDoc = await db.collection('doctors').doc(recipientId).get();
      if (doctorDoc.exists && doctorDoc.data()?.fcmToken) {
        fcmToken = doctorDoc.data().fcmToken;
        isDoctor = true;
      }
    }

    if (!fcmToken) {
      logger.warn(`No FCM token found for recipient ${recipientId}`);
      return;
    }

    // 3. Dispatch the high-priority push notification
    const payload = {
      token: fcmToken,
      notification: {
        title: senderName,
        body: text,
      },
      data: {
        type: 'chat_message',
        chatId: chatId,
        senderId: senderId,
        senderName: senderName,
        isDoctor: isDoctor ? 'true' : 'false',
        text: text,
        timestamp: String(messageData.timestamp ? messageData.timestamp.toMillis() : Date.now()),
      },
      android: {
        priority: 'high',
      },
    };

    await getMessaging().send(payload);
    logger.log(`Chat notification sent to ${recipientId} from ${senderName}`);
  } catch (error) {
    logger.error('Error sending chat notification:', error);
  }
});