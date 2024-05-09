
## Prerequisites
- An available Ubuntu Server 24.04 LTA VM with specs equivalent to AWS' t2.xlarge specifications

## Testing MTAv6.2.2 workflow
- Clone the serverless-workflows repo which includes the code to ci tests
```shell
ssh <your ubuntu vm>
git clone https://github.com/rhkp/serverless-workflows.git -b flpath751
cd serverless-workflows/experimental-ci-on-ubuntu-vm/
chmod +x ubuntuvm.sh
chmod +x mtav6.2.2.sh
chmod +x cluster-up.sh
chmod +x janus-idp.sh
chmod +x koveyor-operator-0.2.1.sh
```

- Setup the ubuntu vm with required prerequisite software components
```shell
./ubuntuvm.sh
```

- Test the MTAv6.2.2 workflow
```shell
./mtav6.2.2.sh
```