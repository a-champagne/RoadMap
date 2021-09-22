#!/usr/bin/env python
  
#import bluetooth
from __future__ import print_function
import time as currtime
import serial
import os
import subprocess
import json
import firebase_admin
from firebase_admin import credentials
from firebase_admin import db
from threading import Thread, Lock

cred = credentials.Certificate('cpen391-test-firebase-adminsdk-76343-74bca15157.json')
default_app = firebase_admin.initialize_app(cred, {'databaseURL' : 'https://cpen391-test.firebaseio.com' } )


configPath = "/home/pi/.vehicle_config" #path to configuration file

root = db.reference()

mutex = Lock()

VEHICLE_ID = None
driver_id = None
current_trip = -1
violation_flag = False
zone_violation_timestamp = None
last_msg_id = None

def update_zone_violation(data, status):
    timestamp = data["KEY_TIME"]
    lat = data["KEY_LATITUDE"]
    lon = data["KEY_LONGITUDE"]
    if status == True: 
        status = "active"
        global zone_violation_timestamp 
        zone_violation_timestamp = timestamp
    else:
        status = "inactive"
    
    violation_data = {"DriverID": driver_id, "Status": status, "TimeStamp": timestamp, "TripID": current_trip, "VehicleID" : VEHICLE_ID, "Location": { "latitude": lat, "longitude": lon} }
    
    root.child("Zone_Violations/" + zone_violation_timestamp + "/").update(violation_data)
    
    

def check_zone_violation(data):
    
    lat = data["KEY_LATITUDE"]
    lon = data["KEY_LONGITUDE"]   
    
    zone_coords = root.child("Trips/"+ str(current_trip) + "/Zone/").get()
    # print (zone_coords)
    if lat < zone_coords["topLeft"]["latitude"] and lat > zone_coords["bottomRight"]["latitude"] and lon > zone_coords["topLeft"]["longitude"] and lon < zone_coords["topRight"]["longitude"]:
        return False
    else:
        return True 
     
        


def vehicle_loc_update(data):
    timestamp = str(data["KEY_TIME"])
    
    loc_log = {timestamp : { "latitude": data["KEY_LATITUDE"], "longitude": data["KEY_LONGITUDE"], "speed": data["KEY_SPEED"], "N_S": data["KEY_NS"], "E_W": data["KEY_EW"] }}
   
    root.child("Vehicles/" + VEHICLE_ID +"/Location/").update({ "latitude": data["KEY_LATITUDE"], "longitude": data["KEY_LONGITUDE"], "speed": data["KEY_SPEED"], "N_S": data["KEY_NS"], "E_W": data["KEY_EW"] })
    root.child("Vehicles/" + VEHICLE_ID +"/Location_Log/").update(loc_log)
    # print("location updated")

def parse_gps(nmea_string):
    if nmea_string.startswith('$GPRMC'):
        nmea_split = nmea_string.split(",")
        
        if nmea_split[2] == 'A':  
            lat = float(nmea_split[3]) / 100
            lat = int(lat) + (lat - int(lat))/0.6
            
            ns = str(nmea_split[4])
            if ns == "S":
                lat *= -1
                
            lon = float(nmea_split[5]) / 100
            lon = int(lon) + (lon - int(lon))/0.6          
            ew = str(nmea_split[6])
            if ew == "W":
                lon *= -1
                
            speed = float(nmea_split[7]) * 1.852001
            time = str(currtime.time())
            time = time[:-3]
   
            data = {"REQUEST_TYPE": "EVENT_LOCATION_UPDATE",
                "KEY_LATITUDE": lat,
                "KEY_NS": ns,
                "KEY_LONGITUDE": lon,
                "KEY_EW": ew,
                "KEY_SPEED": speed,
                "KEY_TIME": time,
                "KEY_VIOLATION_STATUS": violation_flag}
            json_data = json.dumps(data)
            
            
            return json_data
        else: 
            return None
                
    else: 
        return None

def bluetooth_tx(data):

    #mutex.acquire()
    try:
        bt.write("###" + data + "@@@")
        print("BT Data Written:")
        print(data)
    except Exception as e:
        print("error! serial device not available")
        print(e)

    #finally:
        #mutex.release()

def emergency(data):
    trip_id = data["DATA"]["TripID"]
    
    data["DATA"]["VehicleID"] = VEHICLE_ID
    json_emerg = json.dumps(data["DATA"])
   # print(json_emerg)
    root.child("Emergency_Alerts/" + str(trip_id)).update(data["DATA"])
   
def maintenance(data):

    print("\n\nSending maintenance request to Firebase")
    print(data["DATA"])
    timestamp = str(data["DATA"]["TimeStamp"])
    root.child("Maintenance_Alerts/" + VEHICLE_ID +"/" + timestamp).update(data["DATA"])
    

def location_data(gps_data):
    bluetooth_tx(gps_data)
    #print("Sent Location: ")
    #print(gps_data)

def route_data(data, gps_data):

    print("Getting all my trips from Firebase")
    driver_id = data["DATA"]["DriverID"]
    alltrips = root.child("Trips/").get()
    #json.loads(alltrips)

    selected_trips = list()

    if isinstance(alltrips, list) != True:
        keys = alltrips.keys()
        for key in keys:
            trip = alltrips[key]

            try:
                if trip["vehicleID"] == VEHICLE_ID:
                    selected_trips.append(trip)
            except Exception as e:
                print(e)
    else:
        for trip in alltrips:

            try:
                if trip == None:
                    continue
                elif trip["vehicleID"] == VEHICLE_ID:
                    selected_trips.append(trip)
            except Exception as e:
                print(e)

    
    # print(selected_trips)

    trips_json = json.dumps( {"REQUEST_TYPE": "BT_CONNECTED", "TRIPS": selected_trips, "VehicleID": VEHICLE_ID} )
 

    bluetooth_tx(trips_json)

def listen_for_messages(event):

    print("Incoming Message")
    message = event.data

    if message == None:
        return

    largest = sorted(message.keys())[-1]
    
    json_fu = {}
    json_fu["REQUEST_TYPE"] = "EVENT_SINGLE_MESSAGE"
   # json_fu["DATA"] = event.data[largest]
    json_fu["DATA"] = event.data

    bluetooth_tx(json.dumps(json_fu))


    print("End of Message")


def choose_trip(data):

    print("Setting chosen trip in Firebase")
    trip_id = data["DATA"]["TripID"]
    driver_id = data["DATA"]["DriverID"]
    timestamp = data["DATA"]["TimeStamp"]
    
    print("Trip Chosen")
    print(trip_id)
    global current_trip 
    current_trip = int(trip_id)
    print (current_trip)

    update_trip = {'driverID' : driver_id, 'TimeStamp' : timestamp, "active": True}
    update_vehicle = {"TripID" : trip_id, "Available" : False}

    root.child("Trips/" + str(trip_id)).update(update_trip) 
    root.child("Vehicles/" + VEHICLE_ID).update(update_vehicle)
    
    root.child("Trips/" + str(current_trip) + "/Messages").listen(listen_for_messages)


def stop_trip(data):

    trip_id = data["DATA"]["TripID"]
    end_time = data["DATA"]["TimeStamp"]

    update_trip = { 'active': False, "finish": end_time }
    update_vehicle = {"Available" : True}

    root.child("Trips/" + str(trip_id)).update(update_trip)
    root.child("Vehicles/" + VEHICLE_ID).update(update_vehicle)


    bluetooth_tx(json.dumps({"REQUEST_TYPE": "BT_STOP_TRIP"}))
    #root.child("Trips/" + str(current_trip) + "/Messages").off()

 
 
def getVehicleIdFromFile():

    print("Getting Vehicle ID from file")

    with open(configPath, 'r') as configFile:
        data = configFile.read()

    vehicle = json.loads(data)
    vehicleID = str(vehicle["vehicleID"])

    print("Vehicle ID is: " + vehicleID)

    global VEHICLE_ID
    VEHICLE_ID = vehicleID

    return


def updateVehicleID(data):

    print("Updating VehiceleID")
    newID = data["DATA"]["VehicleID"]


    with open(configPath, "r") as configFile:
        data = configFile.read()

    vehicleData = json.loads(data)
    vehicleData["vehicleID"] = newID

    with open(configPath, "w") as configFile:
        configFile.write(json.dumps(vehicleData, indent = 4))

    print("Updated vehicleID to: " + newID)

    global VEHICLE_ID
    VEHICLE_ID = newID

    json_str = json.dumps({ "REQUEST_TYPE" : "BT_UPDATE_ID", "DATA" : VEHICLE_ID})

    bluetooth_tx(json_str)

    return

def send_message(data):

    print("Sending message to Firebase")
    message_data = data["DATA"]["Message_Data"]
    user_type = data["DATA"]["fromUserType"]
    timestamp = data["DATA"]["TimeStamp"]

    global trip_id

    root.child("Trips/" + str(current_trip) + "/Messages/" + str(timestamp)).update( {'Message_Data' : message_data, 'ReadStatus': False, 'fromUserType': user_type, 'TimeStamp' : timestamp} )

    # MAYBE NEED TO GET MESSAGES AFTER SENDING BUT PROBABLY NEY
    # get_messages_data = {"DATA": {"TripID": trip_id} }
    # get_messages(get_messages_data)

def get_messages(data):

    print("Getting Messages from Firebase")
    trip_id = current_trip

    messages = root.child("Trips/" + str(trip_id) + "/Messages").get()

    print(messages)

    if messages == None:
        return

    message_list = list()
    keys = sorted(messages.keys())
    for key in keys:
        trip = messages[key]
        try:
            message_list.append({'key': key, 'value': trip})
        except Exception as e:
            print(e)
    
    messages_json = json.dumps( {"REQUEST_TYPE": "EVENT_MESSAGE", "MESSAGES": message_list} )
    bluetooth_tx(messages_json)





#def message(data):

    #'''
    #TODO: Write this function
    #- Load last msg received ID from phone
    #- Load incoming message from phone
    #- Check for new messages on db, and load all new from database to PI to send to phone
    #- Send new messages to phone, update last message received  ID on phone
    #- Send incoming message to database
    #- check sent/received status of last message
    #-
    #'''




def request_handler(request, gps_data):
    
    print("\n\nPARSED REQUEST")
    request_type = request["REQUEST_TYPE"]

    print("\t" + str(request_type))
    print(request)


    if request_type == "BT_EMERGENCY":
        emergency(request)
    elif request_type == "BT_LOCATION_DATA":
        location_data(gps_data)
    elif request_type == "BT_CONNECTED":
        route_data(request, gps_data)
    elif request_type == "BT_MESSAGE":
        send_message(request)
    elif request_type == "BT_GET_MESSAGES":
        get_messages(request)
    elif request_type == "BT_MAINTENANCE":
        maintenance(request)
    elif request_type == "BT_CHOOSETRIP":
        choose_trip(request)
    elif request_type == "BT_UPDATE_ID":
        updateVehicleID(request)
    elif request_type == "BT_STOP_TRIP":
        stop_trip(request)
    else:
        print("Request not recognized")

#~ def update_location(data):

ser = serial.Serial(
      
    port='/dev/ttyS0',
    baudrate = 9600,
    parity=serial.PARITY_NONE,
    stopbits=serial.STOPBITS_ONE,
    bytesize=serial.EIGHTBITS,
    timeout=1
   )

bt = serial.Serial(
      
    port='/dev/ttyUSB0',
    baudrate = 115200,
    parity=serial.PARITY_NONE,
    stopbits=serial.STOPBITS_ONE,
    bytesize=serial.EIGHTBITS,
    timeout=1
   )



#Make sure this is the first thing that runs
getVehicleIdFromFile()

counter=0
print ("Waiting for bluetooth connection")  

gps_json_data = None
app_request = list()
app_json_request = list()
curr_time = 0


while 1:
   
    try:
        gps_raw = ser.readline()
       # print(gps_raw)
    except Exception as e:
        print("Bluetooth device not available")

    gps_json_data = parse_gps(gps_raw)
    
    if gps_json_data != None:
        loc_data = json.loads(gps_json_data)
        timestamp = loc_data["KEY_TIME"]
        
        if current_trip != -1:
            violation_status = check_zone_violation(loc_data)
        
        if current_trip != -1 and violation_status != violation_flag:
            violation_flag = violation_status
            update_zone_violation(loc_data, violation_status) 
            
      
        if long(timestamp) - long(curr_time) >= 10:
            vehicle_loc_update(loc_data)
            curr_time = timestamp 
            
    while bt.in_waiting:
        try:
            app_request.append(bt.readline())
            # print (app_request)
            for req in app_request:
                json_req = json.loads(req)
                app_request.remove(req)
                app_json_request.append(json_req)
            
        except Exception as e:
            print("Error: Android application JSON String not read")
            print (app_request)
            print(e)
            app_request = list()

    if gps_json_data != None and app_request != None:
        for req in app_json_request:
            request_handler(req, gps_json_data);
        app_request = list()
        app_json_request = list()
        gps_json_data = None



  




