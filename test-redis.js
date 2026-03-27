const { createClient } = require('redis');
const client = createClient({
  url: 'rediss://default:ATg-AAIncDFjMDQ3ZDg0Nzg3YjA0YWE3OGEzOTg4OWNhMDkwN2JiZXAxMTQzOTg@pro-rodent-14398.upstash.io:6379'
});
client.on('error', err => console.log('Redis Client Error', err));
client.connect().then(() => {
  console.log('Connected!');
  client.quit();
});
