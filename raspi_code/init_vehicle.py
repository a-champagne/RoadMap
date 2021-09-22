import os
import json

fileName = ".vehicle_config"
dirPath = "/home/pi/"

configPath = dirPath + fileName

exists = os.path.isfile(configPath)

if exists:
	print("Vehicle already configured!")

        #with open(configPath, "r") as configFile:
else:
	print("Configuring Vehicle!")
        with open(configPath, "w+") as configFile:
	
            data = {}
	    data['vehicleID'] = 'GB857N'
	    json_data = json.dumps(data, indent = 4)
	    configFile.write(json_data)
        
        os.chmod(configPath, 0o666)

