# Dublin Weather

Simple Weather App, as an example project 

Functional points

1. UI is fairly primitive, but the idea is that when you click on the main weather, or one of the five days (beneath), you will be brought to a new screen activity with an expanded view of all weather stats for that day. 
2. A Dark theme maybe preferable, so the colors inverted would look better (also a vertical line separation between the 5 days)
3. Currently hardcoded to Dublin, IE, but as requirements it should defaults to your current location, just didn't have enough time to incorporate location based services, etc, and setup permission-ing etc
4. There are a lot of data points in the feed, I took the midday temp (or greater) for a given day (from the first Weather element of each day). Didn't have enough time to analyse the model data, but this is really driven from I think is most appropriate for the average weather consumer.
5. Plenty of more additions, but that's the main stuff for now.


Technical points

1. UI Tests are missing (only some core unit tests were developed). Note also I setup a fake interceptor for testing purposes, so I can test the domain object creation etc.
2. Currently the Activity is directly connected to Network Service (Job scheduler), it would have been better to offload that logic to a separate component, however it still needs to be tied to application/main activity life cycle. This separation is more relevant in a larger application, where the main activity gets overloaded with functionality.


Technical Design:
1. This sample application is broken into the following high level elements UI, JobScheduler Service, DataBroker and Network. 
The Network module hides the details of the network connectivity, and data retrieval/parsing. 
The DataBroker, provides a means for the UI layer and JobScheduler to get data, and also encapsulates the logic to determine where to retrieve the data (cache or from the network)
The JobScheduler is responsible for polling the databroker (who returns live data) according to the defined periodic timeframe (however there is no guarantee of this occuring on time, due to phone and network conditions)
2. The data package represents the data elements returned from the weather feed. These objects are just generated directly mapped from JSON. For the purpose of this application these are the Model elements, but in a larger application we would take the data elements and marshall them into ViewModel elements (i.e. data that is only relevant to the UI screen)
3. A raw file is used to persist data, but strategically it would be better to encapsulate this in a structured repository (no-sql, etc). Also further design improvements would include a persistence data layer, e.g. View->Model->DataReader->Persistence Store.
JobScheduler->DataWriter->Persistence Store.

