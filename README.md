
# Transit Movements Validator
This is an internal microservice to parse and validate XML payloads. 

## Repository 
- [Project Github Page](https://github.com/hmrc/transit-movements-validator)

## Monitoring

### Kibana
- [Kibana Logs](https://kibana.tools.production.tax.service.gov.uk/app/kibana#/dashboard/transit-movements-validator?_g=()&_a=(description:%27%27,filters:!(),fullScreenMode:!f,options:(),panels:!((gridData:(h:20,i:%271%27,w:24,x:0,y:0),id:transit-movements-validator-http-requests,panelIndex:%271%27,type:visualization,version:%276.8.23%27),(embeddableConfig:(columns:!(container_id,request),sort:!(%27@timestamp%27,desc)),gridData:(h:20,i:%272%27,w:24,x:0,y:20),id:transit-movements-validator-http-requests,panelIndex:%272%27,type:search,version:%276.8.23%27),(gridData:(h:20,i:%273%27,w:24,x:24,y:0),id:transit-movements-validator-app-exceptions,panelIndex:%273%27,type:visualization,version:%276.8.23%27),(embeddableConfig:(columns:!(app,host,exception),sort:!(%27@timestamp%27,desc)),gridData:(h:20,i:%274%27,w:24,x:24,y:20),id:transit-movements-validator-app-exceptions,panelIndex:%274%27,type:search,version:%276.8.23%27),(gridData:(h:10,i:%275%27,w:24,x:0,y:40),id:transit-movements-validator-ecs-container-notifications,panelIndex:%275%27,type:visualization,version:%276.8.23%27),(embeddableConfig:(columns:!(message,service,service_version),sort:!(%27@timestamp%27,desc)),gridData:(h:10,i:%276%27,w:24,x:24,y:40),id:transit-movements-validator-ecs-container-notifications,panelIndex:%276%27,type:search,version:%276.8.23%27),(gridData:(h:10,i:%277%27,w:24,x:0,y:50),id:transit-movements-validator-container-kill,panelIndex:%277%27,type:visualization,version:%276.8.23%27),(embeddableConfig:(columns:!(service,version,host),sort:!(%27@timestamp%27,desc)),gridData:(h:10,i:%278%27,w:24,x:24,y:50),id:transit-movements-validator-container-kill,panelIndex:%278%27,type:search,version:%276.8.23%27),(gridData:(h:10,i:%279%27,w:24,x:0,y:60),id:transit-movements-validator-service-downstreams,panelIndex:%279%27,type:visualization,version:%276.8.23%27),(embeddableConfig:(columns:!(http_host,request,status),sort:!(%27@timestamp%27,desc)),gridData:(h:10,i:%2710%27,w:24,x:24,y:60),id:transit-movements-validator-service-downstreams,panelIndex:%2710%27,type:search,version:%276.8.23%27),(gridData:(h:10,i:%2711%27,w:24,x:0,y:70),id:transit-movements-validator-service-upstreams,panelIndex:%2711%27,type:visualization,version:%276.8.23%27),(embeddableConfig:(columns:!(http_user_agent,request,status),sort:!(%27@timestamp%27,desc)),gridData:(h:10,i:%2712%27,w:24,x:24,y:70),id:transit-movements-validator-service-upstreams,panelIndex:%2712%27,type:search,version:%276.8.23%27)),query:(language:kuery,query:%27%27),timeRestore:!f,title:transit-movements-validator,viewMode:view))
- [Kibana Repository](https://github.com/HMRC/kibana-dashboards) 


### Grafana
- [Grafana Graphs](https://grafana.tools.production.tax.service.gov.uk/d/R5w4CHw7z/transit-movements-validator?orgId=1&refresh=15)
- [Grafana Repository](https://github.com/HMRC/grafana-dashboards)

### Pager Duty
- TBD

## Documentation
- [Service Catalog](https://catalogue.tax.service.gov.uk/service/transit-movements-validator)
- [PRA Confluence Page](https://confluence.tools.tax.service.gov.uk/display/DTRG/Common+Transit+Convention+%28CTC%29+Traders+API+Phase+5+-+Validator)

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
