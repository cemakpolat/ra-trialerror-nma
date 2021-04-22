package com.gtarc.network.config;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import com.gtarc.network.utils.Console;

/**
 * 
 * @author cemakpolat, doruksahinel
 *
 */
public class ServiceTestConfig {

	// user info
	public static String group1 = "group1";
	public static String group2 = "group2";
	
	public static int userGroup1Number = 10;
	public static int userGroup2Number = 10;
	
	public static int totalUserGroup = 2;
	public static int totalUserNumber = 30;


	public static int iterationNumber = 1;

	ServiceTestConfig() {

	}

	public ServiceTestConfig setUserGroupNumber(int value) {
		totalUserGroup = value;
		return this;
	}

	public ServiceTestConfig setUserNumber(int value) {
		totalUserNumber = value;
		return this;
	}

	public int getGroupNumber() {
		return totalUserGroup;
	}

	public int getUserNumber() {
		return totalUserNumber;
	}
	public void setGroupUserNumber(String groupName,int userNumber) {
		if(groupName.equalsIgnoreCase(group1)) {
			userGroup1Number = userNumber;
		} else if(groupName.equalsIgnoreCase(group2)) {
			userGroup2Number = userNumber;
		}
	}
	public int getGroupNumber(String groupName) {
		if(groupName.equalsIgnoreCase(group1)) {
			return userGroup1Number;
		} else if(groupName.equalsIgnoreCase(group2)) {
			return userGroup2Number;
		}
		Console.output("ERROR:GroupName could not be found!");
		return 0;
	}
	public String configFolder = "configs";
	public String configFile ="ServiceConfig.txt";
	public void readConfigFile() {
		String fileNameFullPath = configFolder+"/"+configFile;	
		try {
			FileReader input = new FileReader(fileNameFullPath);
			BufferedReader bufRead = new BufferedReader(input);
            String line; 
            line = bufRead.readLine();
            if(line != null && !line.contains("##") && !line.isEmpty()) {
        			assignParameter(line);
            }
			// Read through file one line at time. Print line # and line
            while (line != null){
                //System.out.println(count+": "+line);
                line = bufRead.readLine();
                if(line != null && !line.contains("##") && !line.isEmpty()) {
            			assignParameter(line);
                }
            }
            bufRead.close();
			
        }catch (ArrayIndexOutOfBoundsException e){
			System.out.println("Usage: java ReadFile filename\n");			

		}catch (IOException e){
            e.printStackTrace();
        }
	}

	public void assignParameter(String value) {
		String[] param = value.split("=");
		switch(param[0].replaceAll("\\s","")) {
			case "userGroupNumber":
				String[] groups = param[1].replaceAll("\\s","").split(",");
				for(int i = 0 ;i < groups.length ; i ++) {
					String[] group = groups[i].split(":");
					if(group[0].replaceAll("\\s","").equalsIgnoreCase("group1")) {
						this.setGroupUserNumber(group[0].replaceAll("\\s",""), Integer.parseInt(group[1].replaceAll("\\s","")));		
					} else if(group[0].replaceAll("\\s","").equalsIgnoreCase("group2")) {
						this.setGroupUserNumber(group[0].replaceAll("\\s",""), Integer.parseInt(group[1].replaceAll("\\s","")));
					}
				}
				break;
			case "totalUserGroup":
				this.setUserGroupNumber(Integer.parseInt(param[1].replaceAll("\\s","")));
				break;
			case "totalUserNumber": 
				this.setUserNumber(Integer.parseInt(param[1].replaceAll("\\s","")));
				break;
			default:
				Console.output("Unknown variable!");
		}
	}
	public static void main(String[] args){
		ServiceTestConfig config = new ServiceTestConfig();
		config.readConfigFile();
	}
	
}
