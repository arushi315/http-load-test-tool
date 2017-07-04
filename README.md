# http-load-test-tool
Load test tool allows user to make HTTP request for given parameters. Built based on Vertx &amp; Java 8.

- Sample input JSON to trigger the load test.

        {
            "durationInSeconds" : 700000,
            "useBasicAuth" : true,
            "basicAuthUser" : "test",
            "basicAuthPassword" : "test",
            "rampUpTimeInSeconds" : 20,
            "testType" : "REQUEST_PER_SECOND",
            "path" : "/Microsoft-Server-ActiveSync",
        	"remoteHosts" : [
        		"https://localhost:8443"
        	],
        	"commonParameters" : [
        		{
        			"name" : "DeviceId",
        			"value" : "###"
        		},
        		{
        			"name" : "DeviceType",
        			"value" : "###"
        		},
        		{
        			"name" : "User",
        			"value" : "###"
        		}
        	],
            "remoteOperations" : [
            	{
            		"operationType" : "Ping",
        	    	"loadRequestsPerSecond" : 5, 
        	    	"parameters" : [
        	    		{
        	    			"name" : "Cmd",
        	    			"value" : "Ping"
        	    		}
        	    	]
            	},
            	{
            		"operationType" : "Sync",
        	    	"loadRequestsPerSecond" : 5, 
        	    	"parameters" : [
        	    		{
        	    			"name" : "Cmd",
        	    			"value" : "Sync"
        	    		}
        	    	]
            	},
            	{
            		"operationType" : "ItemOperations",
        	    	"loadRequestsPerSecond" : 1, 
        	    	"parameters" : [
        	    		{
        	    			"name" : "Cmd",
        	    			"value" : "ItemOperations"
        	    		}
        	    	]
            	}
            ]
        }