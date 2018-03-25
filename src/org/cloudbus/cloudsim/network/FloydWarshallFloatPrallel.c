#include <jni.h>
#include <stdio.h>
#include "org.cloudbus.cloudsim.network.FloydWarshallFloatPrallel.h"
#include <float.h>
#include <omp.h>
#include <stdlib.h>
#include <limits.h>


#define Oriented 1 /* if oriented graph T <-1, else T<- 0 */

jint **flow;
jfloat **dist_f;

jint *LS, *LP, *F;

void printDist(jint n,jfloat **dis) {
	int i, j;

	printf("\t");
	for (i = 0; i < n; ++i)
		printf("\t%4c", 'A' + i);

	printf("\n");
	for (i = 0; i < n; ++i) {
		printf("\t%4c", 'A' + i);
		for (j = 0; j < n; ++j){
			if (dis[i][j]==FLT_MAX)
				printf("\t%4s", "Inf");
			else
				printf("\t%4.1f", dis[i][j]);
		}
			
		printf("\n");
	}
	printf("\n");
}

void printFlow(jint n,jint **flw) {
	int i, j;

	printf("\t");
	for (i = 0; i < n; ++i)
		printf("\t%4c", 'A' + i);

	printf("\n");
	for (i = 0; i < n; ++i) {
		printf("\t%4c", 'A' + i);
		for (j = 0; j < n; ++j){
			if (flw[i][j]==INT_MAX)
				printf("\t%4s", "Inf");
			else
				printf("\t%4d", flw[i][j]);
		}
			
		printf("\n");
	}
	printf("\n");
}

void init_dist_f_flow(jint n, jfloat** dis){
	jint i,j;
	dist_f = (jfloat**) malloc(n*sizeof(jfloat*));
	flow   = (jint**) malloc(n*sizeof(jint*));

	for(i = 0; i < n; ++i){
		dist_f[i] = (jfloat*) malloc(n*sizeof(jfloat));
		flow[i]   = (jint*) malloc(n*sizeof(jint));

		for(j = 0; j < n; ++j){
			//creation d'une sauvegarde de dist
			dist_f[i][j]=dis[i][j];

			if(i==j){
				flow[i][j] = 0;
			}

			if(dis[i][j]!=FLT_MAX){
				flow[i][j]=i;
			}else{
				flow[i][j]=INT_MAX;
			}
		}
	}
}

void floyd_warshall(jint n) {
	jint i, j, k, v;

	for (k = 0; k < n; ++k)
    #pragma omp parallel for private(i,j)
		for (i = 0; i < n; ++i){
			if (dist_f[i][k] == FLT_MAX){
				continue;
			}
			if (i==k){
				continue;
			}
			for (j = 0; j < n; ++j){
				if (dist_f[i][k] < dist_f[i][j])
					if(dist_f[k][j] < dist_f[i][j]){
						v = dist_f[i][k]+dist_f[k][j];
						if(v < dist_f[i][j]){
							dist_f[i][j] = v;
							flow[i][j] = flow[k][j];
						}
					}
						
			}
		}
}

/*
void init_LP_LS_F(jint n, jint m, jint** dis){
	jint i,j,indx_lp,indx_ls;

	LP = malloc((n+1)*sizeof(jint));
	LS = malloc(m*sizeof(jint));
	F  = malloc(m*sizeof(jint));

	indx_ls=0;
	for(i=0;i<n;i++){
		LP[i]=indx_ls;
		for(j=Oriented?0:i+1;j<n;j++){
			if(i==j) continue;

			if(dis[i][j]!=INT_MAX){
				LS[indx_ls]=j;
				F[indx_ls]=0;
				indx_ls++;
			}
		}
	}
	LP[n]=m;
}

void print_lp(jint n){
	jint i;
	printf("\n\t*************** LP Table *****************\n");
	for(i = 0; i < n+1; ++i){
		printf("%d%s\t",LP[i],",");
	}
}

void print_ls(jint m){
	jint i;
	printf("\n\t*************** LS Table *****************\n");
	for (i = 0; i < m; ++i){
		printf("%d%s\t",LS[i],",");
	}
}

void print_f(jint m){
	jint i;
	printf("\n\t*************** F Table *****************\n");
	for (i = 0; i < m; ++i){
		printf("%d%s\t",F[i],",");
	}
}

void increment_arc(jint src, jint dst){
	jint kmin, kmax, u;
	if(!Oriented){
		kmin = src <dst ? src :dst;
		kmax = src >=dst? src : dst;
	}else{
		kmin=src;
		kmax=dst;
	}

	
	for(u=LP[kmin];u<LP[kmin+1];u++)
		if(LS[u] == kmax) 
			break;
	
	#pragma omp atomic
		F[u]++;	
}

void computeFlows(jint n){
	jint i;
	
	printf("\n");
	#pragma omp parallel for private(i)
		for(i=0; i<n;i++){
			jint j;
			for(j=0;j<n;j++){
				jint src,dst;
				if(flow[i][j]==INT_MAX) continue;

				if(i==j) continue;

				src =j;
				dst = j;
				while (src!=i){
					src=flow[i][dst];
					increment_arc(src,dst);
					dst=src;
				}
			}
		}
}
*/

//JNIEXPORT void JNICALL Java_HelloWorld_floyd(JNIEnv *env, jobject obj, jint n,jint m, jobjectArray dist){
JNIEXPORT void JNICALL Java_FloydWarshallFloatPrallel_floyd(JNIEnv *env, jobject obj, jint n,jint m, jobjectArray dist, jobject lock){
	double start,stop,start2,stop2;

	int i, asize;
	jfloat** localArray;
	jfloatArray* oneDim; 
	jobjectArray ret;
	jclass cls;
	jmethodID mid;
	jfieldID fid;
	jint arraySize;
	
	//n -> nombre de noeuds, n-> nombre d'arcs et dist -> la matrice de distances
	//recupération des données de distances
	printf("n=%d\n",n);
	printf("m=%d\n",m);

	oneDim = malloc(n*sizeof(jfloatArray*));
	localArray = malloc(n*sizeof(jfloat*));

	for (i = 0; i < n; ++i){
		oneDim[i] = (*env)->GetObjectArrayElement(env,dist,i);
		localArray[i] = (*env)->GetFloatArrayElements(env,oneDim[i],NULL);
	}



	printf("\t%s\n","---------------- dist ------------------------" );
	printDist(n,localArray);

	//initialiser la matrice des shortest paths + la matrice flow
	init_dist_f_flow(n, localArray);

	printf("\t%s\n","--------------  dist_f  ----------------------" );
	printDist(n,dist_f);


	printf("\t%s\n","---------------- flow ------------------------" );
	printFlow(n,flow);
	

	//calcul des shortest paths
	start2 = omp_get_wtime();
	floyd_warshall(n);
	stop2 = omp_get_wtime();

	/*
	 * return the shortest paths values and the flow matrix
	 */
	jfloatArray row = (jfloatArray)(*env)->NewFloatArray(env,n);
	ret=(jobjectArray)(*env)->NewObjectArray(env,n,(*env)->GetObjectClass(env,row),0);

	for(i=0;i<n;i++){
		row = (jfloatArray)(*env)->NewFloatArray(env,n);
		(*env)->SetFloatArrayRegion(env,(jfloatArray)row,(jsize)0,n,(jfloat *)localArray[i]);
		(*env)->SetObjectArrayElement(env,ret,i,row);
	}

	cls=(*env)->GetObjectClass(env,obj);
	mid=(*env)->GetMethodID(env,cls,"sendArrayResults","([[F)V");

	if(mid == 0){
		printf("Can't find method sendArrayResults\n");
		return;
	}

	(*env)->ExceptionClear(env);
	(*env)->MonitorEnter(env,lock);
	(*env)->CallVoidMethod(env,obj, mid, ret);
	(*env)->MonitorExit(env,lock);

	if((*env)->ExceptionOccurred(env)){
		printf("Error occured copying array back\n");
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}

	fid=(*env)->GetFieldID(env,cls,"arraySize","I");
	if(fid==0){
		printf("Can't find field arraySize\n");
		return;
	}

	asize=(*env)->GetIntField(env,obj,fid);
	if(!(*env)->ExceptionOccurred(env)){
		printf("Java array size=%d\n", asize);
	}else{
		(*env)->ExceptionClear(env);
	}
	
	printf("\nEnd!\n");
	return;

}
