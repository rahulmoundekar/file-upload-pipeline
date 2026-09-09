import http from 'k6/http';
import { check } from 'k6';
export const options={vus:20,duration:'30s'};
export default function(){const r=http.get('http://localhost:8083/actuator/health');check(r,{'health ok':x=>x.status===200});}
