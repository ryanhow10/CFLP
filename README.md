# CFLP

**NOTE:** This program makes use of the [Gurobi Library](https://www.gurobi.com).

---
### The Capacitated Facility Location Problem (CFLP)
The [Capacitated Facility Location Problem](https://en.wikipedia.org/wiki/Facility_location_problem#Capacitated_facility_location) is concerned with the optimal placement of facilities in order to minimize costs associated with transportation as well as operating the various facilities while satisfying customer demand requests. 

This implementation deals with
- a set of commodities/products
- a set of production plants
- a set of potential candidate facilities locations (discrete set)
- a set of customers with demand

The goal is to select a specified number of facility locations such that customer demands are satisfied and total cost is minimized. This must be achieved while keeping in mind the activity level and capacity constraints of the various locations.

With these types of problems, demand can either be non-divisible (single allocation), meaning the demand of the customers is provided by a single facility or demand can be divisible (multiple allocation), meaning the demand of the customers can be fulfilled by multiple facilities.

---
### 2-Echelon Multi-Commodity Single Allocation Model
The first model is a 2-echelon multi-commodity single allocation model. 2-echelon means that both inward and outward flows from the facilities are of interest. The model is concerned with a set of products (multi-commodity) and the demands of these products can be fulfilled by a single facility (single allocation). The full model is displayed below. 
<p align="center">
<img width="635" alt="Screen Shot 2021-09-05 at 4 41 58 PM" src="https://user-images.githubusercontent.com/48066840/132140932-447e33c2-7ce6-4bad-a43f-6d8a006db0a5.png">
</p>
<p align="center">
<img width="635" alt="Screen Shot 2021-09-05 at 4 48 13 PM" src="https://user-images.githubusercontent.com/48066840/132141026-27cb6c4c-c1fb-4422-bfac-4d72720d9322.png">
</p>

---
### 2-Echelon Multi-Commodity Model with Divisible Demand
In this variation of the CFLP, the model is similar to the one displayed above but instead of customer demand being satisfied by only a single facility, it can be split amongst multiple facilities. Customers can therefore receive supply from multiple facilities. The full model is displayed below. 
<p align="center">
<img width="635" alt="Screen Shot 2021-09-05 at 4 49 20 PM" src="https://user-images.githubusercontent.com/48066840/132141063-7c515692-e24a-44e1-84fb-fbebaf917401.png">
</p>
<p align="center">
<img width="635" alt="Screen Shot 2021-09-05 at 4 49 45 PM" src="https://user-images.githubusercontent.com/48066840/132141077-dad2e757-cf28-4eb9-bbb0-000007274402.png">
</p>

---
### Execution
There are a total of 11 arguments needed for the program which are listed below.

1. File with customers demands for products
<p align="center">
<img width="632" alt="Screen Shot 2021-09-05 at 4 56 19 PM" src="https://user-images.githubusercontent.com/48066840/132141248-6d76cf42-d247-4397-9994-15071acd7684.png">
</p>

2. File with production plant capacities
<p align="center">
<img width="632" alt="Screen Shot 2021-09-05 at 4 56 27 PM" src="https://user-images.githubusercontent.com/48066840/132141247-2ad4609d-bf62-47c6-b645-c2febfbd441c.png">
</p>

3. File with minimum activity level for facilities
<p align="center">
<img width="633" alt="Screen Shot 2021-09-05 at 4 56 34 PM" src="https://user-images.githubusercontent.com/48066840/132141246-52b8a375-da03-46ae-970f-7bdc4c38cb99.png">
</p>

4. File with maximum activity level for facilities
<p align="center">
<img width="630" alt="Screen Shot 2021-09-05 at 4 56 43 PM" src="https://user-images.githubusercontent.com/48066840/132141245-4d7fd7a0-9bfb-43f2-abc0-691b76e1bde5.png">
</p>

5. File with fixed cost for facilities
<p align="center">
<img width="630" alt="Screen Shot 2021-09-05 at 4 56 52 PM" src="https://user-images.githubusercontent.com/48066840/132141244-0ba6c899-f403-4a68-ab6b-fd495ec77d3e.png">
</p>

6. File with marginal cost for facilities
<p align="center">
<img width="631" alt="Screen Shot 2021-09-05 at 4 56 59 PM" src="https://user-images.githubusercontent.com/48066840/132141243-bff43ae5-adc3-4e4e-b9cb-0ecabf514f2e.png">
</p>

7. File with unit transportation cost for products
<p align="center">
<img width="633" alt="Screen Shot 2021-09-05 at 4 57 06 PM" src="https://user-images.githubusercontent.com/48066840/132141242-a8154294-9849-40ce-bedc-2e3182ea027c.png">
</p>

8. File with distances from plants to facilities
<p align="center">
<img width="631" alt="Screen Shot 2021-09-05 at 4 57 14 PM" src="https://user-images.githubusercontent.com/48066840/132141241-77975db5-070c-4464-9bf3-48087b642117.png">
</p>

9. File with distances frorm facilities to customers
<p align="center">
<img width="632" alt="Screen Shot 2021-09-05 at 4 57 27 PM" src="https://user-images.githubusercontent.com/48066840/132141240-4ab01c46-505b-44d3-9d44-4e81d45fe2e0.png">
</p>

---
### Example
Displayed below is an example scenario that demonstrates the CFLP. There exist 2 production plants, 3 potential facility locations and 2 customers. The example files can be found in the root of the directory. The 10th argument is the value of 2 to represent 2 open facilities and the 11th argument will first be "single" and then "divisible".
<p align="center">
<img width="650" src="https://user-images.githubusercontent.com/48066840/132141490-78788f24-0411-4e1b-bae7-159c39295f3b.png">
</p>

**LEGEND:** The thicker arrows represent product 1 and the thinner arrows represent product 2.

#### Single Allocation Model Solution  
<p align="center">
<img width="650" src="https://user-images.githubusercontent.com/48066840/132141526-34bc36ad-8030-4162-ba2e-1adba8595582.png">
</p>

#### Divisible Demand Model Solution
<p align="center">
<img width="650" src="https://user-images.githubusercontent.com/48066840/132141555-45c9bd72-1db1-44ed-a10c-9b40257ae317.jpg">
</p>
