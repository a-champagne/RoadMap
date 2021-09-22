import json


configPath = "/home/pi/.vehicle_config"
print("Hello from tester!")


with open(configPath, 'r') as configFile:
    data = configFile.read()


vehicle = json.loads(data)

print("Old VehicleID: " + str(vehicle["vehicleID"]))

vehicle["vehicleID"] = "CHANGED"

with open(configPath, "w") as configFile:
    configFile.write(json.dumps(vehicle, indent =4))


print("New VehicleID: " + str(vehicle["vehicleID"]))
