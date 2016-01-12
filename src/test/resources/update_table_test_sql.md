# Single Row Insert
## ALL list - <font color=green>PASSING</font>
### First try
```sql
insert into Block set ID='1block', color='#aabbcc';
insert into update_table set rowID='1block', tableName='Block', type='INSERT';
```

### Second try
```sql
insert into Block set ID='2block', color='#8899aa';
insert into update_table set rowID='2block', tableName='Block', type='INSERT';
```

## Referenced list - <font color=green>PASSING</font>
### First try
```sql
insert into Circle set ID='1circle', color='#11aa22', person_ID='e3d04ee4-e996-4f4a-9c5e-7e87e5685849';
insert into update_table set rowID='1circle', tableName='Circle', type='INSERT';
```

### Second try
```sql
insert into Circle set ID='2circle', color='#22bb33', person_ID='e3d04ee4-e996-4f4a-9c5e-7e87e5685849';
insert into update_table set rowID='2circle', tableName='Circle', type='INSERT';
```

# Whole Table Insert
## ALL list - <font color=green>PASSING</font>
### First try
```sql
insert into Block set ID='3block', color='#a3b3c3';
insert into Block set ID='4block', color='#a4b4c4';
insert into update_table set rowID=null, tableName='Block', type='INSERT';
```

### Second try
```sql
insert into Block set ID='5block', color='#b5b5c5';
insert into Block set ID='6block', color='#b6b6c6';
insert into update_table set rowID=null, tableName='Block', type='INSERT';
```

## Reference list - <font color=green>PASSING</font>

```sql
-- First Try
insert into Circle set ID='3circle', color='#ccc333', person_ID='e3d04ee4-e996-4f4a-9c5e-7e87e5685849';
insert into Circle set ID='4circle', color='#ccc444', person_ID='e3d04ee4-e996-4f4a-9c5e-7e87e5685849';
insert into update_table set rowID=null, tableName='Circle', type='INSERT';

-- Second try
insert into Circle set ID='5circle', color='#ccc555', person_ID='e3d04ee4-e996-4f4a-9c5e-7e87e5685849';
insert into Circle set ID='6circle', color='#ccc666', person_ID='e3d04ee4-e996-4f4a-9c5e-7e87e5685849';
insert into update_table set rowID=null, tableName='Circle', type='INSERT';
```

# Single Row Update
## Value update - <font color=green>PASSING</font>
```sql
-- First Try --
update Block set color='#333333' where ID='1' ;
insert into update_table set rowID='1', tableName='Block', type='UPDATE';

-- Second Try --
update Block set color='#aaaaaa' where ID='1' ;
insert into update_table set rowID='1', tableName='Block', type='UPDATE';
```

## Reference List - <font color=green>PASSING</font>

Make sure that when updating references that objects are added/removed the appropriate lists

```sql
-- First Try -- 
update Circle set color='green', person_ID='a11df72c-9aa5-4f75-ba41-ce4c4539fdbe' where ID='6circle';
insert into update_table set rowID='6circle', tableName='Circle', type='UPDATE';

-- Second Try -- 
update Circle set color='red', person_ID='e3d04ee4-e996-4f4a-9c5e-7e87e5685849' where ID='6circle';
insert into update_table set rowID='6circle', tableName='Circle', type='UPDATE';
```

# Whole Table update
## Value update - <font color=green>PASSING</font>
```sql
-- First Try -- 
update Block set color='coral';
insert into update_table set rowID=null, tableName='Block', type='UPDATE';

-- Second Try --
update Block set color='lightcoral';
insert into update_table set rowID=null, tableName='Block', type='UPDATE';
```

## Reference List - 
```sql
-- First Try --
update Circle set person_ID='' where person_ID='a11df72c-9aa5-4f75-ba41-ce4c4539fdbe';
update Circle set person_ID='a11df72c-9aa5-4f75-ba41-ce4c4539fdbe' where person_ID='e3d04ee4-e996-4f4a-9c5e-7e87e5685849';
update Circle set person_ID='e3d04ee4-e996-4f4a-9c5e-7e87e5685849' where person_ID='';
insert into update_table set rowID=null, tableName='Circle', type='UPDATE';

-- Second try --
update Circle set person_ID=null where person_ID='a11df72c-9aa5-4f75-ba41-ce4c4539fdbe';
update Circle set person_ID='a11df72c-9aa5-4f75-ba41-ce4c4539fdbe' where person_ID='e3d04ee4-e996-4f4a-9c5e-7e87e5685849';
update Circle set person_ID='e3d04ee4-e996-4f4a-9c5e-7e87e5685849' where person_ID=null;
insert into update_table set rowID=null, tableName='Circle', type='UPDATE';
```